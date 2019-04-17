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

object ConnHelper extends Serializable {
  lazy val jedis = new Jedis("localhost")
  lazy val mongoClient = MongoClient(MongoClientURI("mongodb://localhost:27017/recommender"))
}

case class MongConfig(uri: String, db: String)

//推荐
case class Recommendation(rid: Int, r: Double)

// 用户的推荐
case class UserRecs(userId: Int, recs: Seq[Recommendation])

//商品的相似度
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
    val ssc = new StreamingContext(sc, Seconds(2))

    implicit val mongConfig = MongConfig(config("mongo.uri"),config("mongo.db"))
    import spark.implicits._

    //******************  广播商品相似度矩阵

    // 为了性能考虑，把相似度矩阵这个大数据广播出去；广播变量的好处：不是每个task一份变量副本，而是变成每个节点的executor才一份副本
    // 转换成为 Map[Int, Map[Int, Double]]
    // (pid1, [(pid2, sim2), (pid3, sim3), ...])
    // {
    //    pid1: {
    //         pid2: simscore,
    //         pid3: simscore,
    //    },
    //    pid2: {
    //         pid3: simscore,
    //    }
    // }
    // 和pid1相似的, O(1)

    val simProductsMatrix = spark
      .read
      .option("uri",config("mongo.uri"))
      .option("collection", MONGODB_PRODUCT_RECS_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[ProductRecs]
      .rdd
      .map {recs =>
        (recs.productId, recs.recs.map(x => (x.rid, x.r)).toMap)
      }.collectAsMap()

    simProductsMatrix.foreach(println)

    val simProductsMatrixBroadCast = sc.broadcast(simProductsMatrix)

    //创建到Kafka的连接
    val kafkaPara = Map(
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "recommender",
      "auto.offset.reset" -> "latest"    //每次从kafka消费数据，都是通过zookeeper存储的数据offset，来判断需要获取消息在消息日志里的起始位置
    )

    val kafkaStream = KafkaUtils.createDirectStream[String,String](ssc,LocationStrategies.PreferConsistent,ConsumerStrategies.Subscribe[String,String](Array(config("kafka.topic")),kafkaPara))

    // uid|pid|rating|timestamp
    val ratingStream = kafkaStream.map{case msg =>
      var attr = msg.value().split("\\|")            // split方法对. | * + ^需要转义（类似正则）
      (attr(0).toInt, attr(1).toInt, attr(2).toDouble, attr(3).toInt)
    }

    ratingStream.foreachRDD{rdd =>
      rdd.map{case (userId, productId, score, timestamp) =>
        println(">>>>>>>>>>>>>>>>")

        // 获取当前最近的M次商品评分
        val userRecentlyRatings = getUserRecentlyRating(MAX_USER_RATINGS_NUM, userId, ConnHelper.jedis)

        // 获取商品P最相似的K个商品
        val simProducts = getTopSimProducts(MAX_SIM_PRODUCTS_NUM, productId, userId, simProductsMatrixBroadCast.value)

        // 计算待选商品的推荐优先级
        val streamRecs = computeProductScores(simProductsMatrixBroadCast.value, userRecentlyRatings, simProducts)

        // 将数据保存到MongoDB
        saveRecsToMongoDB(userId, streamRecs)

      }.count()
    }

    //启动Streaming程序
    ssc.start()
    ssc.awaitTermination()
  }

  def saveRecsToMongoDB(userId: Int, streamRecs: Array[(Int,Double)])(implicit mongConfig: MongConfig): Unit = {
    //到StreamRecs的连接
    val streamRecsCollection = ConnHelper.mongoClient(mongConfig.db)(MONGODB_STREAM_RECS_COLLECTION)

    streamRecsCollection.findAndRemove(MongoDBObject("userId" -> userId))
    //streaRecsCollection.insert(MongoDBObject("userId" -> userId, "recs" -> streamRecs.map(x=> x._1+":"+x._2).mkString("|")))
    streamRecsCollection.insert(MongoDBObject("userId" -> userId, "recs" -> streamRecs.map(x => MongoDBObject("productId" -> x._1, "score" -> x._2))))

  }

  def computeProductScores(simProducts: scala.collection.Map[Int,scala.collection.immutable.Map[Int,Double]], userRecentlyRatings: Array[(Int,Double)],topSimProducts: Array[Int]): Array[(Int,Double)] = {

    //用于保存每一个待选商品和最近评分的每一个商品的权重得分的数组
    val score = scala.collection.mutable.ArrayBuffer[(Int, Double)]()

    //用于保存每一个商品的增强因子数
    val increMap = scala.collection.mutable.HashMap[Int, Int]()

    //用于保存每一个商品的减弱因子数
    val decreMap = scala.collection.mutable.HashMap[Int, Int]()

    for (topSimProduct <- topSimProducts; userRecentlyRating <- userRecentlyRatings) {
      val simScore = getProductsSimScore(simProducts, userRecentlyRating._1, topSimProduct)
      if(simScore > 0) {
        score += ((topSimProduct, simScore * userRecentlyRating._2))
        if(userRecentlyRating._2 > 3) {
          increMap(topSimProduct) = increMap.getOrDefault(topSimProduct, 0) + 1
        } else {
          decreMap(topSimProduct) = decreMap.getOrDefault(topSimProduct, 0) + 1
        }
      }
    }

    score.groupBy(_._1).map{case (productId, sims) =>
      (productId, sims.map(_._2).sum / sims.length + log(increMap.getOrDefault(productId, 1)) - log(decreMap.getOrDefault(productId, 1)))
    }.toArray

  }

  //取2的对数
  def log(m: Int): Double = {
    math.log(m) / math.log(2)
  }

  def getProductsSimScore(simProducts: scala.collection.Map[Int,scala.collection.immutable.Map[Int,Double]], userRatingProduct: Int, topSimProduct: Int): Double = {
    // topSimProduct是候选商品q
    // userRatingProduct是最近评分过的一个商品R_r
    simProducts.get(topSimProduct) match {
      case Some(sim) => sim.get(userRatingProduct) match {
        case Some(score) => score
        case None => 0.0
      }
      case None => 0.0
    }
  }

  def getTopSimProducts(num: Int, productId: Int, userId: Int, simProducts: scala.collection.Map[Int,scala.collection.immutable.Map[Int,Double]])(implicit mongConfig: MongConfig): Array[Int] = {
    //从广播变量的商品相似度矩阵中获取当前商品所有的相似商品
    val allSimProducts = simProducts(productId).toArray
    //获取用户已经评分过的商品
    val ratingExist = ConnHelper.mongoClient(mongConfig.db)(MONGODB_RATING_COLLECTION).find(MongoDBObject("userId" -> userId)).toArray.map{item =>
      item.get("productId").toString.toInt
    }
    //过滤掉已经评分过的商品，并排序输出
    allSimProducts.filter(x => !ratingExist.contains(x._1)).sortWith(_._2 > _._2).take(num).map(x => x._1)
  }

  def getUserRecentlyRating(num: Int, userId: Int, jedis: Jedis): Array[(Int, Double)] = {
    //从用户的队列中取出num个评分
    jedis.lrange("userId:" + userId.toString, 0, num).map{item =>
      val attr = item.split("\\:")
      (attr(0).trim.toInt, attr(1).trim.toDouble)
    }.toArray
  }

}
