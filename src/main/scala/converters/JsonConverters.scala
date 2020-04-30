package converters

import java.time.LocalDate

import spray.json.{DeserializationException, JsString, JsValue, JsonFormat}

object JsonConverters {
  implicit object LocalDateJsonFormat extends JsonFormat[LocalDate] {
    override def write(localDate: LocalDate): JsValue = JsString(localDate.toString)

    override def read(json: JsValue): LocalDate = json match {
      case JsString(value) =>
        val date = value.split("-").map(_.toInt)
        LocalDate.of(date(0), date(1), date(2))
      case _ => throw DeserializationException("Date expected")
    }
  }
}
