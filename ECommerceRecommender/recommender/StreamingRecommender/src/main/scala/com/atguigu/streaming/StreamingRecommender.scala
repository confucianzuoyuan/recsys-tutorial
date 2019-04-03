package com.atguigu.streaming

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.JavaConversions._

object ConnHelper extends Serializable{
  lazy val jedis = new Jedis("localhost")
  lazy val mongoClient = MongoClient(MongoClientURI("mongodb://localhost:27017/recommender"))
}

case class MongConfig(uri: String, db: String)

//推荐
case class Recommendation(rid: Int, r: Double)

// 用户的推荐
case class UserRecs(uid: Int, recs: Seq[Recommendation])

//电影的相似度
case class ProductRecs(productId: Int, recs: Seq[Recommendation])

object StreamingRecommender {

  val MAX_USER_RATINGS_NUM = 20
  val MAX_SIM_PRODUCTS_NUM = 20
  val MONGODB_STREAM_RECS_COLLECTION = "StreamRecs"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_PRODUCT_RECS_COLLECTION = "ProductRecs"

  //入口方法
  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender",
      "kafka.topic" -> "recommender"
    )
    //创建一个SparkConf配置
    val sparkConf = new SparkConf().setAppName("StreamingRecommender").setMaster(config("spark.cores"))

    //创建Spark的对象, 因为spark session中没有封装streaming context，所以需要new一个
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    val sc = spark.sparkContext
    sc.setLogLevel("ERROR")
    val ssc = new StreamingContext(sc,Seconds(2))

    implicit val mongConfig = MongConfig(config("mongo.uri"),config("mongo.db"))
    import spark.implicits._

    //******************  广播电影相似度矩阵

    // 为了性能考虑，把相似度矩阵这个大数据广播出去；广播变量的好处：不是每个task一份变量副本，而是变成每个节点的executor才一份副本
    //装换成为 Map[Int, Map[Int,Double]]
    // (mid1, [(mid2, sim2), (mid3, sim3), ...])
    // {
    //    mid1: [(mid2, sim2), (mid3, sim3), ...]
    // }
    // 和mid1相似的, O(1)
    val simProductsMatrix = spark
      .read
      .option("uri",config("mongo.uri"))
      .option("collection",MONGODB_PRODUCT_RECS_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[ProductRecs]
      .rdd
      .map{recs =>
        (recs.productId, recs.recs.map(x => (x.rid,x.r)).toMap)
      }.collectAsMap()  // 把元组转换为map，以mid为key，推荐列表为value

    val simProductsMatrixBroadCast = sc.broadcast(simProductsMatrix)

    val abc = sc.makeRDD(1 to 2)
    abc.map(x => simProductsMatrixBroadCast.value.get(1)).count()

    //******************


    //创建到Kafka的连接
    val kafkaPara = Map(
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "recommender",
      "auto.offset.reset" -> "latest"    //每次从kafka 消费数据，都是通过zookeeper存储的数据offset，来判断需要获取消息在消息日志里的起始位置
    )

    // r.lpush(1) r.lpop()

    val kafkaStream = KafkaUtils.createDirectStream[String,String](ssc,LocationStrategies.PreferConsistent,ConsumerStrategies.Subscribe[String,String](Array(config("kafka.topic")),kafkaPara))

    // UID|MID|SCORE|TIMESTAMP
    // 1|1000|4.5|timestamp
    // 产生评分流
    val ratingStream = kafkaStream.map{case msg =>
      var attr = msg.value().split("\\|")            // split方法对. | * + ^需要转义（类似正则）
      (attr(0).toInt, attr(1).toInt, attr(2).toDouble, attr(3).toInt)
    }

    ratingStream.foreachRDD{rdd =>
      rdd.map{case (userId, productId, score, timestamp) =>
        println(">>>>>>>>>>>>>>>>")

        //获取当前最近的M次电影评分
        val userRecentlyRatings = getUserRecentlyRating(MAX_USER_RATINGS_NUM, userId, ConnHelper.jedis)

        //获取电影P最相似的K个电影
        val simProducts = getTopSimProducts(MAX_SIM_PRODUCTS_NUM, productId, userId, simProductsMatrixBroadCast.value)

        //计算待选电影的推荐优先级
        val streamRecs = computeProductScores(simProductsMatrixBroadCast.value, userRecentlyRatings, simProducts)

        //将数据保存到MongoDB
        saveRecsToMongoDB(userId, streamRecs)

      }.count()
    }

    //启动Streaming程序
    ssc.start()
    ssc.awaitTermination()
  }

  /**
    * 将数据保存到MongoDB    uid -> 1,  recs -> 22:4.5|45:3.8
    * @param streamRecs  流式的推荐结果
    * @param mongConfig  MongoDB的配置
    */
  def saveRecsToMongoDB(uid:Int,streamRecs:Array[(Int,Double)])(implicit mongConfig: MongConfig): Unit ={
    //到StreamRecs的连接
    val streamRecsCollection = ConnHelper.mongoClient(mongConfig.db)(MONGODB_STREAM_RECS_COLLECTION)

    streamRecsCollection.findAndRemove(MongoDBObject("uid" -> uid))
    //streaRecsCollection.insert(MongoDBObject("uid" -> uid, "recs" -> streamRecs.map(x=> x._1+":"+x._2).mkString("|")))
    streamRecsCollection.insert(MongoDBObject("uid"->uid, "recs"-> streamRecs.map(x => MongoDBObject("mid"->x._1, "score"->x._2)) ))

  }

  /**
    * 计算待选电影的推荐分数
    * @param simMovies            电影相似度矩阵
    * @param userRecentlyRatings  用户最近的k次评分
    * @param topSimMovies         当前电影最相似的K个电影
    * @return
    */
  def computeProductScores(simProducts: scala.collection.Map[Int,scala.collection.immutable.Map[Int,Double]],userRecentlyRatings:Array[(Int,Double)],topSimProducts: Array[Int]): Array[(Int,Double)] = {

    //用于保存每一个待选电影和最近评分的每一个电影的权重得分
    val score = scala.collection.mutable.ArrayBuffer[(Int,Double)]()

    //用于保存每一个电影的增强因子数
    val increMap = scala.collection.mutable.HashMap[Int,Int]()

    //用于保存每一个电影的减弱因子数
    val decreMap = scala.collection.mutable.HashMap[Int,Int]()

    for (topSimProduct <- topSimProducts; userRecentlyRating <- userRecentlyRatings){
      val simScore = getProductsSimScore(simProducts, userRecentlyRating._1, topSimProduct)
      if(simScore > 0.6){
        score += ((topSimProduct, simScore * userRecentlyRating._2 ))
        if(userRecentlyRating._2 > 3){
          increMap(topSimProduct) = increMap.getOrDefault(topSimProduct,0) + 1
        }else{
          decreMap(topSimProduct) = decreMap.getOrDefault(topSimProduct,0) + 1
        }
      }
    }

    score.groupBy(_._1).map{case (mid,sims) =>
      (mid,sims.map(_._2).sum / sims.length + log(increMap.getOrDefault(mid, 1)) - log(decreMap.getOrDefault(mid, 1)))
    }.toArray

  }

  //取2的对数
  def log(m: Int): Double ={
    math.log(m) / math.log(2)
  }

  /**
    * 获取当个电影之间的相似度
    * @param simMovies       电影相似度矩阵
    * @param userRatingMovie 用户已经评分的电影
    * @param topSimMovie     候选电影
    * @return
    */
  //{
  //  mid1: {mid2: rating2, mid3: rating3}
  //}
  def getProductsSimScore(simProducts: scala.collection.Map[Int,scala.collection.immutable.Map[Int,Double]], userRatingProduct: Int, topSimProduct: Int): Double ={
    simProducts.get(topSimProduct) match {
      case Some(sim) => sim.get(userRatingProduct) match {
        case Some(score) => score
        case None => 0.0
      }
      case None => 0.0
    }
  }

  /**
    * 获取当前电影K个相似的电影
    * @param num          相似电影的数量
    * @param mid          当前电影的ID
    * @param uid          当前的评分用户
    * @param simMovies    电影相似度矩阵的广播变量值
    * @param mongConfig   MongoDB的配置
    * @return
    */
  def getTopSimProducts(num: Int, productId: Int, userId: Int, simProducts:scala.collection.Map[Int,scala.collection.immutable.Map[Int,Double]])(implicit mongConfig: MongConfig): Array[Int] ={
    //从广播变量的电影相似度矩阵中获取当前电影所有的相似电影
    val allSimProducts = simProducts(productId).toArray
    //获取用户已经观看过得电影
    val ratingExist = ConnHelper.mongoClient(mongConfig.db)(MONGODB_RATING_COLLECTION).find(MongoDBObject("userId" -> userId)).toArray.map{item =>
      item.get("productId").toString.toInt
    }
    //过滤掉已经评分过得电影，并排序输出
    allSimProducts.filter(x => !ratingExist.contains(x._1)).sortWith(_._2 > _._2).take(num).map(x => x._1)
  }

  /**
    * 获取当前最近的M次电影评分
    * @param num  评分的个数
    * @param uid  谁的评分
    * @return
    */
  def getUserRecentlyRating(num: Int, userId: Int, jedis: Jedis): Array[(Int, Double)] ={
    //从用户的队列中取出num个评论
    jedis.lrange("userId:" + userId.toString, 0, num).map{item =>
      val attr = item.split("\\:")
      (attr(0).trim.toInt, attr(1).trim.toDouble)
    }.toArray
  }

}
