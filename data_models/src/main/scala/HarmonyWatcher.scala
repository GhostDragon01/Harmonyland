package data_models

import play.api.libs.json._

case class HarmonyWatcher(id: Int, location: Location)

object HarmonyWatcher {
  implicit val harmonyWatcherFormat: Format[HarmonyWatcher] = Json.format[HarmonyWatcher]
}