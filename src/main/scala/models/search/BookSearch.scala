package models.search

import java.time.LocalDate

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class BookSearch(title: Option[String] = None,
                      releaseDate: Option[LocalDate] = None,
                      categoryId: Option[Long] = None, // TODO replace by category
                      author: Option[String] = None
                     )

trait BookSearchJson extends SprayJsonSupport with DefaultJsonProtocol {
  import converters.JsonConverters.LocalDateJsonFormat

  implicit val bookSearchFormat = jsonFormat4(BookSearch.apply)
}
