package data_models

import play.api.libs.json._
import scala.util.Random

case class Location(latitude: Double, longitude: Double)

object Location {

  // Create a random Location object
  def createRandomLocation(): Location = {
    Location(Random.between(-90.00, 90.00), Random.between(-180.00, 180.00))
  }

  implicit val locationFormat: Format[Location] = Json.format[Location]
}