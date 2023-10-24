package data_models

import play.api.libs.json._

import java.time.LocalDateTime

case class Report(harmonyWatcher: HarmonyWatcher, persons: List[Person], words: List[String], time: LocalDateTime)

object Report {
  implicit val reportFormat: Format[Report] = Json.format[Report]
}