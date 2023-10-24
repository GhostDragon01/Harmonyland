package data_models

import play.api.libs.json._
case class Person(name: String, age: Int, gender: String, harmonyScore: Int)
object Person {
  implicit val personFormat: Format[Person] = Json.format[Person]
}