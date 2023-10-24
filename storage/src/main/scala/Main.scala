package storage

import data_models._
import play.api.libs.json._

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies._
import org.apache.spark.SparkConf
import org.apache.log4j._

object Main {

  def main(args: Array[String]): Unit = {

    // Remove logs from sparks
    Logger.getRootLogger.setLevel(Level.OFF)

    //access and secret key for AWS S3
    val accessKey = ""
    val secretKey = ""

    // Create a local StreamingContext with two working thread and batch interval of 10 second.
    // The master requires 2 cores to prevent a starvation scenario.
    // local[*] is to run in local mode
    val sparkConf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("storage_s3")
      .set("spark.driver.host", "127.0.0.1")
    val streamContext = new StreamingContext(sparkConf, Seconds(10))
    val spark: SparkSession = SparkSession.builder.config(streamContext.sparkContext.getConf).getOrCreate()

    // Set up access Keys
    spark.sparkContext.hadoopConfiguration.set("fs.s3a.access.key", accessKey)
    spark.sparkContext.hadoopConfiguration.set("fs.s3a.secret.key", secretKey)
    spark.sparkContext.hadoopConfiguration.set("fs.s3a.endpoint", "s3.amazonaws.com")

    // Define the Kafka configuration parameters as a Map
    val kafkaParams = Map(
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "storage"
    )

    /* ------- Consume the topic 'Harmony state' to store the reports' ------- */
    val topicReport = Array("HarmonyState")

    // Get the stream flux
    val streamReport = KafkaUtils.createDirectStream[String, String](
      streamContext,
      PreferConsistent,
      Subscribe[String, String](topicReport, kafkaParams)
    )

    // Get the bucket path to read and write data
    val bucketPath = "s3a://harmonystatebucket/reports/"

    // identify the format of the files saved in the bucket
    val fileExtension = "json"

    // Write into the AWS S3 as a json file
    streamReport
      .flatMap(record => {
        val report = Json.parse(record.value()) // Deserialize the record.value() into a Report object using a JSON library
        Some(Json.toJson(report)) // Convert the Report object to a JSON value
      })
      .map(jsonValue => Json.stringify(jsonValue)) // Convert the JSON value to a string
      .saveAsTextFiles(bucketPath, fileExtension)

    /* ------- Consume the topic 'Alerts' to store the alerts' ------- */
    val topicAlert = Array("Alert")

    // Get the stream flux
    val streamAlert = KafkaUtils.createDirectStream[String, String](
      streamContext,
      PreferConsistent,
      Subscribe[String, String](topicAlert, kafkaParams)
    )

    // Get the bucket path to read and write data
    val bucketPathAlert = "s3a://harmonystatebucket/alerts/"

    // Write into the AWS S3 as a json file
    streamAlert
      .flatMap(record => {
        val report = Json.parse(record.value()) // Deserialize the record.value() into a Report object using a JSON library
        Some(Json.toJson(report)) // Convert the Report object to a JSON value
      })
      .map(jsonValue => Json.stringify(jsonValue)) // Convert the JSON value to a string
      .saveAsTextFiles(bucketPathAlert, fileExtension)

    streamContext.start()
    streamContext.awaitTermination()
  }
}