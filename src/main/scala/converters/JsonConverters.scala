package converters

import java.sql.Timestamp
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

  implicit object TimestampJsonFormat extends JsonFormat[Timestamp] {
    override def write(timestamp: Timestamp): JsValue = JsString(timestamp.toString)

    override def read(json: JsValue): Timestamp = json match {
      case JsString(value) => Timestamp.valueOf(value)
      case _ => throw DeserializationException("Date expected")
    }
  }
}
