package harmonywatcher_simulation

import java.util.Properties
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer

import scala.io.Source
import play.api.libs.json._

import java.util.{Timer, TimerTask}
import java.time.LocalDateTime
import scala.util.Random
import data_models._

import scala.annotation.tailrec

object Main {

  def main (args: Array[String]): Unit = {
    // Suppose there is 500 Harmony watcher
    val n_harmonyWatcher = 500

    // Settings for the Kafka cluster/Producer
    val props: Properties = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])

    // Create a KafkaProducer
    val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)

    val timer = new Timer()
    // The Task is to Send a report
    val task = new TimerTask {
      override def run(): Unit = {
        sendReport(n_harmonyWatcher, producer)
      }
    }

    // Schedule the task to run every 1 minute
    timer.schedule(task, 0, 60000)

    // Keep the program running indefinitely to simulate the HarmonyWatcher tracking
    runIndefinitely()

    // sert a rien je pense
    producer.close()
  }

  @tailrec
  private def runIndefinitely(): Unit = {
    // Do nothing
    runIndefinitely() // Call the function recursively
  }

  private def sendReport(n_harmonyWatcher: Int, producer: KafkaProducer[String, String]) : Unit = {
    if (n_harmonyWatcher > 0) {
      // Create a Report an convert it to json string
      val report = createReport(n_harmonyWatcher)
      val report_jsonstr = Json.stringify(Json.toJson(report))

      // Create the record and send it.
      val record = new ProducerRecord[String, String]("HarmonyState", "report", report_jsonstr)
      producer.send(record)
      // Recursive call to iterate over all harmonywatchers
      sendReport(n_harmonyWatcher - 1, producer)
    }
  }
  private def createReport(n_harmonyWatcher: Int) : Report = {

    // List of word that can be said
    val filename = "harmonywatcher_simulation/src/main/resources/words.txt"
    val all_words = Source.fromFile(filename).getLines().toList

    // Get the current Harmony Watcher
    val harmonyWatcher = HarmonyWatcher(n_harmonyWatcher, Location.createRandomLocation())

    // Take a file of people in a city and convert it to a List of person.
    val jsonStr = Source.fromFile("harmonywatcher_simulation/src/main/resources/people.json").mkString
    val json = Json.parse(jsonStr)
    val people = json.validate[List[Person]] match {
      case JsSuccess(value, _) => value
      case JsError(errors) => {
        println(s"Failed to parse JSON: $errors")
        List.empty[Person]
      }
    }

    // Shuffle the list and take a random sublist of the desired size (between 2 and 5)
    val words_listen = Random.shuffle(all_words).take(Random.between(2, 6))
    // Shuffle the list and take a random sublist of the desired size (between 5 and 20 )
    val people_around = Random.shuffle(people).take(Random.between(5, 21))

    // Create the harmonyWatcher's report
    Report(harmonyWatcher, people_around, words_listen, LocalDateTime.now())
  }

}