package models

import java.time.LocalDate

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat}

final case class Book(id: Option[Long],
                      title: String,
                      releaseDate: LocalDate,
                      categoryId: Long,
                      quantity: Int,
                      authors: String)

trait BookJson extends SprayJsonSupport with DefaultJsonProtocol with LocalDateJson {
  implicit val bookFormat = jsonFormat6(Book.apply)
}

// TODO maybe extract to trait/object?
trait LocalDateJson extends DefaultJsonProtocol {
  implicit object LocalDateJsonFormat extends JsonFormat[LocalDate] {
    override def write(localDate: LocalDate): JsValue = JsString(localDate.toString)

    override def read(json: JsValue): LocalDate = json match {
      case JsString(value) =>
        val date = value.split("-").map(_.toInt)
        LocalDate.of(date(0), date(1), date(2))
      case _ => throw new DeserializationException("Date expected")
    }
  }
}

trait BookTable {
  class Books(tag: Tag) extends Table[Book](tag, "books") {
    def id: Rep[Option[Long]] = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column[String]("title", O.Unique)
    def releaseDate: Rep[LocalDate] = column[LocalDate]("releaseDate")
    def categoryId: Rep[Long] = column[Long]("categoryId")
    def quantity: Rep[Int] = column[Int]("quantity")
    def authors: Rep[String] = column[String]("authors")

    override def * : ProvenShape[Book] =
      (id, title, releaseDate, categoryId, quantity, authors) <> ((Book.apply _).tupled, Book.unapply)
  }

  protected val books = TableQuery[Books]
}

