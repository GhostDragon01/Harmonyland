import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import play.api.libs.json.{Reads, Writes, Json}
import spray.json.DefaultJsonProtocol._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

case class AlertMessage(id: String, message: String)

object Main {
  implicit val system: ActorSystem = ActorSystem("alert-system")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val alertMessageFormat = jsonFormat2(AlertMessage.apply)
  implicit val alertMessageReads: Reads[AlertMessage] = Json.reads[AlertMessage]
  implicit val alertMessageWrites: Writes[AlertMessage] = Json.writes[AlertMessage]

  val bootstrapServers = "localhost:9092"
  val groupId = "alert-consumer"
  val topic = "Alert"

  val props = new java.util.Properties()
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
  props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
  props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
  props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
  props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(java.util.Collections.singletonList(topic))

  val alertMessages: mutable.Map[String, AlertMessage] = mutable.Map.empty[String, AlertMessage]

  // Define CORS settings
  val corsSettings = CorsSettings.defaultSettings

  // Define the route with CORS settings
  val route = cors(corsSettings) {
    path("alerts") {
      get {
        complete(StatusCodes.OK, alertMessages.values.toList) // Return the list of alert messages
      } ~
        post {
          entity(as[AlertMessage]) { alertMessage =>
            // Handle the alert message here, e.g., save to a database, process, etc.
            println(s"Received alert message: ${alertMessage.message}")
            alertMessages.put(alertMessage.id, alertMessage) // Store the alert message in the map
            complete(StatusCodes.OK, "Alert message received")
          }
        }
    }
  }

  def main(args: Array[String]): Unit = {
    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println("Server online at http://localhost:8080/")

    val task: Runnable = new Runnable {
      override def run(): Unit = {
        val records = consumer.poll(java.time.Duration.ofMillis(100))
        for (record <- records.asScala) {
          val alertMessage = AlertMessage(record.key(), record.value())
          val jsonString = Json.toJson(alertMessage).toString()
          val entity = HttpEntity(ContentTypes.`application/json`, jsonString)
          val request = HttpRequest(
            method = HttpMethods.POST,
            uri = "http://localhost:8080/alerts",
            entity = entity
          )

          val responseFuture = Http().singleRequest(request)
          responseFuture.foreach { response =>
            println(s"Sent alert message to server. Response: ${response.status}")
          }
        }
      }
    }

    // Schedule the task to run every 1 minute
    val interval: FiniteDuration = 30.seconds
    val cancellable: Cancellable = system.scheduler.schedule(0.seconds, interval, task)

    scala.io.StdIn.readLine()

    cancellable.cancel()
    consumer.close()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
    }
}
