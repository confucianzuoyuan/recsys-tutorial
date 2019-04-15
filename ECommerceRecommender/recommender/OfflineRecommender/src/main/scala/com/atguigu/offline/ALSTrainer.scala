package com.atguigu.offline

import breeze.numerics.sqrt
import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object ALSTrainer {

  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender"
    )

    //创建SparkConf
    val sparkConf = new SparkConf().setAppName("ALSTrainer").setMaster(config("spark.cores"))

    //创建SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    import spark.implicits._

    //加载评分数据
    val ratingRDD = spark
      .read
      .option("uri",mongoConfig.uri)
      .option("collection", OfflineRecommender.MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[ProductRating]
      .rdd
      .map(rating => Rating(rating.userId, rating.productId, rating.score)).cache()

    // 训练集的数据量是80%，测试集的数量是20%
    val splits = ratingRDD.randomSplit(Array(0.8, 0.2))

    val trainingRDD = splits(0)
    val testingRDD = splits(1)

    //输出最优参数
    adjustALSParams(trainingRDD, testingRDD)

    //关闭Spark
    spark.close()
  }

  // 输出最终的最优参数
  def adjustALSParams(trainData: RDD[Rating], testData: RDD[Rating]): Unit = {
    val result = for(rank <- Array(30, 40, 50, 60, 70); lambda <- Array(1, 0.1, 0.01, 0.001))
      yield {
        val model = ALS.train(trainData, rank, 5, lambda)
        val rmse = getRmse(model, testData)
        (rank, lambda, rmse)
      }
    println(result.sortBy(_._3).head)
  }

  def getRmse(model: MatrixFactorizationModel, testData: RDD[Rating]): Double = {
    //需要构造一个usersProducts  RDD[(Int,Int)]
    val userProducts = testData.map(item => (item.user, item.product))
    val predictRating = model.predict(userProducts)

    val real = testData.map(item => ((item.user, item.product), item.rating))
    val predict = predictRating.map(item => ((item.user, item.product), item.rating))

    sqrt(
      real.join(predict).map{case ((userId, productId), (real, pre)) =>
        // 真实值和预测值之间的两个差值
        val err = real - pre
        err * err
      }.mean()
    )
  }

}
