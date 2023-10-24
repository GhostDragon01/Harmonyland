package trigger_alert

import data_models._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}

import java.util.Properties
import play.api.libs.json._
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.log4j._

object Main {

  def main(args: Array[String]): Unit = {

    // Remove logs from sparks
    Logger.getRootLogger.setLevel(Level.OFF)

    // Create a local StreamingContext with two working thread and batch interval of 1 second.
    // The master requires 2 cores to prevent a starvation scenario.
    // local[*] is to run in local mode
    val conf = new SparkConf().setMaster("local[*]").setAppName("harmonyState_triggerAlert")
    val ssc = new StreamingContext(conf, Seconds(1))

    // Define the Kafka configuration parameters as a Map
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092", // Kafka bootstrap servers
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "trigger_alert",
      "auto.offset.reset" -> "latest" // Offset reset strategy for consuming from latest offset
    )

    val topics = Array("HarmonyState")

    // Get the stream flux
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    // Settings for the Kafka cluster/Producer
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])

    // Create Kafka producer function
    def createKafkaProducer(): KafkaProducer[String, String] = {
      new KafkaProducer[String, String](props)
    }

    // Basically: Process each RDD -> try to convert to Report Object
    // -> Get the persons with bad harmony score -> Create alerts for those persons
    // -> Send the alerts to Kafka (on a new topic)
    stream.foreachRDD { rdd =>
      // Process each RDD
      rdd.foreach { record =>
        val key = record.key()
        val value = record.value()
        val json_value = Json.parse(value)

        // Convert json to report object
        val reportOpt = json_value.asOpt[Report]

        reportOpt match {
          case Some(report) =>
            val alerts: List[Alert] = report.persons.collect {
              case person if person.harmonyScore < 50 =>
                Alert(report.harmonyWatcher, person, report.time)
            }
          // Send the Alerts to Kafka
            val producer = createKafkaProducer()
            alerts.foreach { alert =>
              val alertJsonString = Json.stringify(Json.toJson(alert))
              val alertRecord = new ProducerRecord[String, String]("Alert", "trigger_alert", alertJsonString)
              producer.send(alertRecord)
            }
            // Close the producer
            producer.close()
          case None =>
          // Handle the case when the JSON value cannot be deserialized into a Report object
          println("JSON value cannot be deserialized into a Report object")
        }
      }
    }
    ssc.start()
    ssc.awaitTermination()
  }
}