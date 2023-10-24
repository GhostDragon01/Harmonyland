package data_models

import play.api.libs.json._
import java.time.LocalDateTime

case class Alert(harmonyWatcher: HarmonyWatcher, person: Person, time: LocalDateTime)

object Alert {
  implicit val alertFormat: Format[Alert] = Json.format[Alert]
}