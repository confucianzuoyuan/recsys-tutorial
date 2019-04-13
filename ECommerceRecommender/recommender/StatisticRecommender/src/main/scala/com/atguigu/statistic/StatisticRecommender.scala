package com.atguigu.statistic

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

case class Product(productId: Int, name: String, categories: String, imageUrl: String, tags: String)

case class Rating(userId: Int, productId: Int, score: Double, timestamp: Int)

case class MongoConfig(uri:String, db:String)

case class Recommendation(rid: Int, r: Double)

object StatisticRecommender {

  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_PRODUCT_COLLECTION = "Products"

  //统计的表的名称
  val RATE_MORE_PRODUCTS = "RateMoreProducts"
  val RATE_MORE_RECENTLY_PRODUCTS = "RateMoreRecentlyProducts"
  val AVERAGE_PRODUCTS = "AverageProducts"

  // 入口方法
  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender"
    )

    // 创建SparkConf配置
    val sparkConf = new SparkConf().setAppName("StatisticRecommender").setMaster(config("spark.cores"))

    // 创建SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    // 调高日志等级
    spark.sparkContext.setLogLevel("ERROR")

    val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    //加入隐式转换
    import spark.implicits._

    //数据加载进来
    val ratingDF = spark
      .read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Rating]
      .toDF()

    val productDF = spark
      .read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_PRODUCT_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Product]
      .toDF()

    ratingDF.createOrReplaceTempView("ratings")

    val rateMoreProductsDF = spark.sql("select productId, count(productId) as count from ratings group by productId")

    rateMoreProductsDF
      .write
      .option("uri", mongoConfig.uri)
      .option("collection", RATE_MORE_PRODUCTS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    val simpleDateFormat = new SimpleDateFormat("yyyyMM")

    spark.udf.register("changeDate", (x: Int) => simpleDateFormat.format(new Date(x * 1000L)).toInt)

    val ratingOfYearMonth = spark.sql("select productId, score, changeDate(timestamp) as yearmonth from ratings")

    ratingOfYearMonth.createOrReplaceTempView("ratingOfMonth")

    val rateMoreRecentlyProducts = spark.sql("select productId, count(productId) as count, yearmonth from ratingOfMonth group by yearmonth, productId")

    rateMoreRecentlyProducts
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",RATE_MORE_RECENTLY_PRODUCTS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    val averageProductsDF = spark.sql("select productId, avg(score) as avg from ratings group by productId order by avg desc")

    averageProductsDF.show()

    averageProductsDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",AVERAGE_PRODUCTS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //关闭Spark
    spark.stop()
  }

}
