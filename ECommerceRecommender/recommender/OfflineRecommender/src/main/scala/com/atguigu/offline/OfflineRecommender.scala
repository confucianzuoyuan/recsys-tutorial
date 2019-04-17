package com.atguigu.offline

import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix

case class Product(productId: Int, name: String, categories: String, imageUrl: String, tags: String)

case class ProductRating(userId: Int, productId: Int, score: Double, timestamp: Int)

case class MongoConfig(uri: String, db: String)

case class Recommendation(rid: Int, r: Double)

case class UserRecs(userId: Int, recs: Seq[Recommendation])

case class ProductRecs(productId: Int, recs: Seq[Recommendation])

object OfflineRecommender {

  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_PRODUCT_COLLECTION = "Products"

  val USER_MAX_RECOMMENDATION = 20

  val USER_RECS = "UserRecs"
  val PRODUCT_RECS = "ProductRecs"

  //入口方法
  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "reommender"
    )

    //创建一个SparkConf配置
    val sparkConf = new SparkConf().setAppName("OfflineRecommender").setMaster(config("spark.cores")).set("spark.executor.memory","6G").set("spark.driver.memory","2G")

    //基于SparkConf创建一个SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    //创建一个MongoDBConfig
    val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    import spark.implicits._

    // 读取mongoDB中的业务数据
    val ratingRDD = spark
      .read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[ProductRating]
      .rdd
      .map(rating => (rating.userId, rating.productId, rating.score)).cache()

    //用户的数据集 RDD[Int]
    val userRDD = ratingRDD.map(_._1).distinct()

    //电影数据集 RDD[Int]
    val productRDD = spark
      .read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGODB_PRODUCT_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Product]
      .rdd
      .map(_.productId).cache()

    //创建训练数据集

    val trainData = ratingRDD.map(x => Rating(x._1, x._2, x._3))

    // r: M x N
    // u: M x K
    // i: K x N
    // 9000 x 100
    // rank参数就是K参数, iterations是迭代次数, lambda是正则化系数
    val (rank, iterations, lambda) = (50, 5, 0.01)
    //训练ALS模型
    val model = ALS.train(trainData, rank, iterations, lambda)

    //计算用户推荐矩阵

    // 需要构造一个usersProducts  RDD[(Int,Int)]
    val userProducts = userRDD.cartesian(productRDD)

    val preRatings = model.predict(userProducts)

    val userRecs = preRatings
      .filter(_.rating > 0)
      // (rating.user, rating.product, rating.rating) => (rating.user, (rating.product, rating.rating))
      .map(rating => (rating.user, (rating.product, rating.rating)))
      .groupByKey()
      .map{
        case (userId, recs) => UserRecs(userId, recs.toList.sortWith(_._2 > _._2).take(USER_MAX_RECOMMENDATION).map(x => Recommendation(x._1, x._2)))
      }.toDF()

    userRecs.write
      .option("uri",mongoConfig.uri)
      .option("collection",USER_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    // productFeatures为物品的隐特征矩阵
    // 每一行：(pid, pid_features)
    val productFeatures = model.productFeatures.map{case (productId, features) =>
      (productId, new DoubleMatrix(features))
    }

    // pid1: vector1; pid2: vector2
    // (a: (pid1, vector1), b: (pid2, vector2)) => (pid1, [(pid2, simScore), [(pid3, simScore)]])
    val productRecs = productFeatures.cartesian(productFeatures)
      .filter{case (a, b) => a._1 != b._1}
      .map{case (a, b) =>
        val simScore = this.consinSim(a._2, b._2)
        (a._1, (b._1, simScore))
      }.filter(_._2._2 > 0)
      .groupByKey()
      .map{case (productId, items) =>
        ProductRecs(productId, items.toList.map(x => Recommendation(x._1,x._2)))
      }.toDF()

    productRecs
      .write
      .option("uri", mongoConfig.uri)
      .option("collection",PRODUCT_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    spark.close()
  }

  def consinSim(product1: DoubleMatrix, product2: DoubleMatrix) : Double = {
    product1.dot(product2) / ( product1.norm2()  * product2.norm2() )
  }

}
