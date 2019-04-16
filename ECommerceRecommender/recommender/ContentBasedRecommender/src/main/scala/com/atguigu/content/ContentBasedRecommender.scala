package com.atguigu.content

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}
import org.apache.spark.ml.linalg.SparseVector
import org.jblas.DoubleMatrix

case class MongoConfig(uri: String, db: String)

case class Product(productId: Int, name: String, categories: String, imageUrl: String, tags: String)
//推荐
case class Recommendation(rid: Int, r: Double)

//商品的相似度
case class ProductRecs(productId: Int, recs: Seq[Recommendation])

object ContentBasedRecommender {
  val MONGODB_PRODUCT_COLLECTION = "Products"
  val PRODUCT_RECS = "ContentBasedProductRecs"

  def consinSim(product1: DoubleMatrix, product2: DoubleMatrix) : Double ={
    product1.dot(product2) / ( product1.norm2() * product2.norm2() )
  }


  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "reommender"
    )

    //创建一个SparkConf配置
    val sparkConf = new SparkConf().setAppName("ContentBasedRecommender").setMaster(config("spark.cores")).set("spark.executor.memory","6G").set("spark.driver.memory","2G")

    //基于SparkConf创建一个SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    //创建一个MongoDBConfig
    val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    import spark.implicits._

    val productRDD = spark
      .read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGODB_PRODUCT_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Product]
      .rdd
      .map(x => (x.productId, x.name, x.tags.map(c => if(c == '|') ' ' else c)))

    val productSeq = productRDD.collect()

    val tagsData = spark.createDataFrame(productSeq).toDF("productId", "name", "tags")

    // 实例化一个分词器，默认按空格分
    // doc: 不好看(10)|送货速度快(3)|很好(100)|质量很好(1)  -> 商品详情
    // tf: 不好看的tf值: 10 / 114
    val tokenizer = new Tokenizer().setInputCol("tags").setOutputCol("words")

    // 用分词器做转换，生成列“words”，返回一个dataframe，增加一列words
    val wordsData = tokenizer.transform(tagsData)

    wordsData.show(5)

    // HashingTF是一个工具，可以把一个词语序列，转换成词频(初始特征)
    val hashingTF = new HashingTF().setInputCol("words").setOutputCol("rawFeatures").setNumFeatures(189)

    // 用 HashingTF 做处理，返回dataframe
    val featurizedData = hashingTF.transform(wordsData)

    // IDF 也是一个工具，用于计算文档的IDF
    val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")

    // 将词频数据传入，得到idf模型（统计文档）
    val idfModel = idf.fit(featurizedData)

    // 模型对原始数据做处理，计算出idf后，用tf-idf得到新的特征矩阵
    val rescaledData = idfModel.transform(featurizedData)

    rescaledData.show(5)

    val productFeatures = rescaledData.map{
      case row => {
        (row.getAs[Int]("productId"), row.getAs[SparseVector]("features").toArray)
      }
    }
    .rdd
    .map(x => {
      (x._1, new DoubleMatrix(x._2))
    })

    val productRecs = productFeatures.cartesian(productFeatures)
      .filter{case (a, b) => a._1 != b._1}
      .map {
        case (a, b) => {
          val simScore = this.consinSim(a._2, b._2)
          (a._1, (b._1, simScore))
        }
      }
      .groupByKey()
      .map {
        case (productId, items) => ProductRecs(productId, items.toList.sortWith(_._2 > _._2).map(x => Recommendation(x._1, x._2)).take(5))
      }
      .toDF()

    productRecs.show(5)

    productRecs
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