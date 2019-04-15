package com.atguigu.itemcf

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

//  物品信息
case class Product(productId: Int, name: String, categories: String, imageUrl: String, tags: String)

case class MongoConfig(uri: String, db: String)

//  用户-物品-评分
case class Rating(userId: Int, productId: Int, score: Double, timestamp: Int)

//  用户信息
case class User(userId: Int)

// rid为推荐商品的id, r为推荐的优先级
case class Recommendation(rid: Int, r: Double)

case class ProductRecs(productId: Int, recs: Seq[Recommendation])

object ItemCFRecommender {
  // 同现相似度计算公式
  // 比如：对A评分的人数100，对B评分的人数100，交集人数20
  // 同现相似度：20 / 100 = 0.2
  def cooccurrence(numOfRatersForAAndB: Long, numOfRatersForA: Long, numOfRatersForB: Long): Double = {
    numOfRatersForAAndB / math.sqrt(numOfRatersForA * numOfRatersForB)
  }

  val MONGODB_PRODUCT_COLLECTION = "Products"
  val MONGODB_RATING_COLLECTION = "Rating"
  val PRODUCT_RECS = "ItemCFProductRecs"


  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "reommender"
    )

    //创建一个SparkConf配置
    val sparkConf = new SparkConf().setAppName("ItemCFRecommender").setMaster(config("spark.cores")).set("spark.executor.memory","6G").set("spark.driver.memory","2G")

    //基于SparkConf创建一个SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    //创建一个MongoDBConfig
    val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    import spark.implicits._

    // 读取mongoDB中的业务数据
    val ratingDF = spark
      .read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Rating]
      .rdd
      .map {
        rating => {
          (rating.userId, rating.productId, rating.score)
        }
      }
      .cache()
      .toDF("userId", "productId", "rating")

    val numRatersPerProduct = ratingDF.groupBy("productId").count().alias("nor")

    // 在原记录基础上加上product的打分者的数量
    // uid1 | pid1 | nor
    // uid2 | pid1 | nor
    val ratingsWithSize = ratingDF.join(numRatersPerProduct, "productId")

    // 执行内联操作
    // select * from ratingsWithSize as r1 inner join ratingsWithSize as r2 on r1.userId=r2.userId
    val joinedDF = ratingsWithSize.join(ratingsWithSize, "userId")
      .toDF("userId", "product1", "rating1", "nor1", "product2", "rating2", "nor2")

    joinedDF
      .selectExpr("userId", "product1", "nor1", "product2", "nor2")
      .createOrReplaceTempView("joined")

    // (uid1, pid1)
    // (uid1, pid2)
    // uid1 | pid1 | nor1Ofpid1 | pid1 | nor1Ofpid1
    // uid1 | pid1 | nor1Ofpid1 | pid2 | nor1Ofpid2
    // uid1 | pid2 | nor1Ofpid2 | pid1 | nor1Ofpid1
    // uid1 | pid2 | nor1Ofpid2 | pid2 | nor1Ofpid2
    // uid2 | pid1 | nor1Ofpid1 | pid2 | nor1Ofpid2

    //  计算必要的中间数据，注意此处有WHERE限定，只计算了一半的数据量
    val sparseMatrix = spark.sql(
      """
        |SELECT product1
        |, product2
        |, count(userId) as size
        |, first(nor1) as nor1
        |, first(nor2) as nor2
        |FROM joined
        |GROUP BY product1, product2
      """.stripMargin)
      .cache()

    // pid1 | pid2 | numofpid1andpid2 | nor1Ofpid1 | nor1Ofpid2

    //  计算物品相似度
    var sim = sparseMatrix.map(row => {
      val size = row.getAs[Long](2)
      val numRaters1 = row.getAs[Long](3)
      val numRaters2 = row.getAs[Long](4)

      val cooc = cooccurrence(size, numRaters1, numRaters2)
      (row.getInt(0), row.getInt(1), cooc)
    }).toDF("productId_01", "productId_02", "cooc")

    // pid1 | pid2 | coocofpid1andpid2
    // pid1 | pid3 | coocofpid1andpid3
    // (pid1, pid2, cooc) => (pid1, (pid2, cooc)) => (pid1, [(pid2, cooc), (pid3, cooc)])

    val simDF = sim
      .map{
        case row => (
          row.getAs[Int]("productId_01"),
          row.getAs[Int]("productId_02"),
          row.getAs[Double]("cooc")
        )
      }
      .rdd
      .map(
        x => (x._1, (x._2, x._3))
      )
      .groupByKey()
      .map {
        case (productId, items) => ProductRecs(productId, items.toList.filter(x => x._1 != productId).sortWith(_._2 > _._2).map(x => Recommendation(x._1, x._2)).take(5))
      }
      .toDF()

    simDF
      .write
      .option("uri", mongoConfig.uri)
      .option("collection", PRODUCT_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //关闭Spark
    spark.close()

  }

}