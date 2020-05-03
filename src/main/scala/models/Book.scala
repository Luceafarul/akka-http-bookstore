package models

import java.time.LocalDate

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import spray.json.DefaultJsonProtocol

final case class Book(id: Option[Long],
                      title: String,
                      releaseDate: LocalDate,
                      categoryId: Long,
                      quantity: Int,
                      authors: String)

trait BookJson extends SprayJsonSupport with DefaultJsonProtocol {
  import converters.JsonConverters.LocalDateJsonFormat

  implicit val bookFormat = jsonFormat6(Book.apply)
}

trait BookTable {
  class Books(tag: Tag) extends Table[Book](tag, "books") {
    def id: Rep[Option[Long]] = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column[String]("title", O.Unique)
    def releaseDate: Rep[LocalDate] = column[LocalDate]("release_date")
    def categoryId: Rep[Long] = column[Long]("category_id")
    def quantity: Rep[Int] = column[Int]("quantity")
    def authors: Rep[String] = column[String]("authors")

    override def * : ProvenShape[Book] =
      (id, title, releaseDate, categoryId, quantity, authors) <> ((Book.apply _).tupled, Book.unapply)
  }

  protected val books = TableQuery[Books]
}

