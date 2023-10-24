package analytics

import data_models._
import play.api.libs.json._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Encoders
import org.apache.spark.sql.types.{ArrayType, DoubleType, IntegerType, StringType, StructField, StructType, TimestampType}
import org.apache.spark.sql.functions.{col, from_json}

import java.time.LocalDateTime
import org.apache.log4j._

import scala.annotation.tailrec
import java.util.{Timer, TimerTask}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import scala.io.StdIn

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
      .setAppName("analytics")
      .set("spark.driver.host", "127.0.0.1")
    val sparkContext = new SparkContext(sparkConf)
    val spark: SparkSession = SparkSession.builder.config(sparkContext.getConf).getOrCreate()

    // Set up access Keys
    spark.sparkContext.hadoopConfiguration.set("fs.s3a.access.key", accessKey)
    spark.sparkContext.hadoopConfiguration.set("fs.s3a.secret.key", secretKey)
    spark.sparkContext.hadoopConfiguration.set("fs.s3a.endpoint", "s3.amazonaws.com")

    val timer = new Timer()
    // The Task is to Send a report
    val task = new TimerTask {
      override def run(): Unit = {
        readFromDatabase(sparkContext)
      }
    }

    // Schedule the task to run every 1 minute
    timer.schedule(task, 0, 60000)

    // Keep the program running indefinitely to simulate the HarmonyWatcher tracking
    runIndefinitely()
  }

  @tailrec
  private def runIndefinitely(): Unit = {
    // Do nothing
    runIndefinitely() // Call the function recursively
  }

  private def readFromDatabase(sparkContext: SparkContext): Unit = {

    // Get the bucket path to read and write data for reports
    val bucketPath = "s3a://harmonystatebucket/reports/*"

    // Get all the reports on a RDD
    val rdd: RDD[String] = sparkContext.textFile(bucketPath)
    val reportRDD: RDD[Report] = rdd.map { line =>
      // Parse the JSON and create a Report object
      val reportJson = Json.parse(line)
      val harmonyWatcher = reportJson("harmonyWatcher").as[HarmonyWatcher]
      val persons = reportJson("persons").as[List[Person]]
      val words = reportJson("words").as[List[String]]
      val time = reportJson("time").as[LocalDateTime]
      Report(harmonyWatcher, persons, words, time)
    }


    // Get the bucket path to read and write data for alerts
    val bucketPathAlert = "s3a://harmonystatebucket/alerts/*"
    // Get all the alerts on an RDD
    val alertRDD: RDD[Alert] = sparkContext.textFile(bucketPathAlert).map { line =>
      // Parse the JSON and create an Alert object
      val alertJson = Json.parse(line)
      val harmonyWatcher = alertJson("harmonyWatcher").as[HarmonyWatcher]
      val person = alertJson("person").as[Person]
      val time = alertJson("time").as[LocalDateTime]
      Alert(harmonyWatcher, person, time)
    }

    /* ------ PART WHERE WE DO THE ANALYTICS WE WANT ------ */

    /* --- Number of reports and number of alerts -- */
    /* --------------------------------------------- */
    val alertCount: Long = alertRDD.count()
    val reportCount: Long = reportRDD.count()
    println(s"Number of reports: $reportCount")
    println(s"Number of alerts: $alertCount")

    /* --- Percentage of alerts that happen in the night -- */
    /* ---------------------------------------------------- */
    val nightAlerts = alertRDD.filter { alert =>
      val time = alert.time
      val hour = time.getHour

      // Filter alerts that occurred between 9 PM and 7 AM
      hour >= 21 || hour <= 7
    }
    val ratioOfNightRiot = (nightAlerts.count().toDouble / alertCount.toDouble) * 100
    println(s"Number of night riot: $ratioOfNightRiot \n\n")

    /* --- Average Alert per report -- */
    val alertAverage = alertCount.toDouble / reportCount.toDouble
    println(s"Average number of Alerts per report: $alertAverage\n\n")

    /* --- Percentage of Alerts generated by age groups -- */
    /* --------------------------------------------------- */
    val ageGroupCounts = alertRDD
      .map(_.person) // Extract the Person objects from each Alert
      .map(_.age) // Extract the age field from each Person
      .map(age => {
        // Assign age groups based on the age value
        val ageGroup = age match {
          case x if x >= 0 && x <= 14 => "0-14"
          case x if x >= 15 && x <= 24 => "15-24"
          case x if x >= 25 && x <= 64 => "25-64"
          case x if x >= 65 && x <= 80 => "65-80"
          case _ => "Unknown"
        }
        (ageGroup, 1)
      })
      .reduceByKey(_ + _) // Count the number of alerts for each age group

    // Calculate the percentage for each age group
    val ageGroupPercentages = ageGroupCounts.mapValues(count => (count.toDouble / alertCount) * 100)

    // Print the percentage for each age group
    ageGroupPercentages.foreach { case (ageGroup, percentage) =>
      println(s"Age Group: $ageGroup, Percentage: $percentage%")
    }

    /* --- Top 3 most pronounced words in reports --- */
    /* ---------------------------------------------- */
    val wordsCount = reportRDD
      .flatMap(_.words) // Flatten the list of words in each report
      .map(word => (word, 1))
      .reduceByKey(_ + _) // Count the occurrence of each word

    val topWords = wordsCount.takeOrdered(3)(Ordering[Int].reverse.on { case (_, count) => count })

    topWords.foreach { case (word, count) =>
      println(s"Word: $word, Count: $count")
    }

    // Create a JsObject with the desired fields
    val jsonData = Json.obj(
      "alertCount" -> alertCount,
      "reportCount" -> reportCount,
      "ratioOfNightRiot" -> ratioOfNightRiot,
      "alertAverage" -> alertAverage,
      "ageGroupPercentages" -> Json.toJson(ageGroupPercentages.collect().toSeq),
      "topWords" -> Json.toJson(topWords)
    )
    // Convert the JsObject to a JSON string
    val jsonString = Json.stringify(jsonData)

    // Create an ActorSystem and Materializer
    implicit val system: ActorSystem = ActorSystem("analytics-system")

    // Define the unmarshaller for HttpEntity.Strict
    implicit val strictEntityUnmarshaller: FromEntityUnmarshaller[HttpEntity.Strict] =
      Unmarshaller.byteStringUnmarshaller
        .forContentTypes(ContentTypes.`application/json`)
        .mapWithCharset { (data, charset) =>
          HttpEntity.Strict(ContentTypes.`application/json`, data)
        }
    // Define CORS settings
    val corsSettings = CorsSettings.defaultSettings

    // Define the route for the HTTP server
    val route = cors(corsSettings) {
      path("data") {
        get {
          complete(StatusCodes.OK, jsonString) // Return the list of alert messages
        } ~
        post {
          entity(as[HttpEntity.Strict]) { entity =>
            val receivedData = entity.data.utf8String
            println(s"Received data: $receivedData")
            complete(StatusCodes.OK)
          }
        }
      }
    }

    // Start the HTTP server
    val bindingFuture = Http().newServerAt("localhost", 8081).bind(route)

    // Send the jsonString to the server
    val jsonRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = "http://localhost:8081/data",
      entity = HttpEntity(ContentTypes.`application/json`, jsonString)
    )

    val responseFuture = Http().singleRequest(jsonRequest)
    responseFuture.foreach { response =>
      println(s"Response status: ${response.status}")
      response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        val responseBody = body.utf8String
        println(s"Response body: $responseBody")
      }
    }

    println("Server online at http://localhost:8081/")

    StdIn.readLine()

    // Stop the server and terminate the ActorSystem
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
    }
}