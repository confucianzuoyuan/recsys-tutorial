package com.atguigu.dataloader

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

case class Product(productId: Int, name: String, categories: String, imageUrl: String, tags: String)

case class Rating(userId: Int, productId: Int, score: Double, timestamp: Int)

case class MongoConfig(uri: String, db: String)

// 数据的主加载服务
object DataLoader {

  val PRODUCTS_DATA_PATH = "/Users/yuanzuo/Desktop/ECommerceRecommender/recommender/DataLoader/src/main/resources/100products.csv"
  val RATING_DATA_PATH = "/Users/yuanzuo/Desktop/ECommerceRecommender/recommender/DataLoader/src/main/resources/9000_users_100_products_ratings.csv"

  val MONGODB_PRODUCT_COLLECTION = "Products"
  val MONGODB_RATING_COLLECTION = "Rating"

  // 程序的入口
  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender"
    )

    val sparkConf = new SparkConf().setAppName("DataLoader").setMaster(config.get("spark.cores").get)

    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    import spark.implicits._

    val productRDD = spark.sparkContext.textFile(PRODUCTS_DATA_PATH)
    val productDF = productRDD.map(item =>{
      val attr = item.split("\\^")
      Product(attr(0).toInt, attr(1).trim, attr(5).trim, attr(4).trim, attr(6).trim)
    }).toDF()

    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
    val ratingDF = ratingRDD.map(item => {
      val attr = item.split(",")
      Rating(attr(0).toInt,attr(1).toInt,attr(2).toDouble,attr(3).toInt)
    }).toDF()

    implicit val mongoConfig = MongoConfig(config.get("mongo.uri").get,config.get("mongo.db").get)

    storeDataInMongoDB(productDF, ratingDF)

    spark.stop()
  }

  // 将数据保存到MongoDB中的方法
  def storeDataInMongoDB(productDF: DataFrame, ratingDF: DataFrame)(implicit mongoConfig: MongoConfig): Unit = {

    //新建一个到MongoDB的连接
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    //如果MongoDB中有对应的数据库，那么应该删除
    mongoClient(mongoConfig.db)(MONGODB_PRODUCT_COLLECTION).dropCollection()
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).dropCollection()

    //将当前数据写入到MongoDB
    productDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection", MONGODB_PRODUCT_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    ratingDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()


    //对数据表建索引
    mongoClient(mongoConfig.db)(MONGODB_PRODUCT_COLLECTION).createIndex(MongoDBObject("productId" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("userId" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("productId" -> 1))

    //关闭MongoDB的连接
    mongoClient.close()
  }
}
