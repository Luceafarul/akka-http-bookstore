package repositories

import models.{Book, BookTable}
import services.DatabaseService

import scala.concurrent.{ExecutionContext, Future}

class BookRepository(databaseService: DatabaseService) (implicit executor: ExecutionContext)
  extends BookTable {


  import databaseService._
  import slick.jdbc.PostgresProfile.api._

  def all: Future[Seq[Book]] = db.run(books.result)

  def create(book: Book): Future[Book] = db.run(books returning books += book)

  def delete(id: Long): Future[Int] = db.run(books.filter(_.id === id).delete)

  def findById(id: Long): Future[Option[Book]] = db.run(books.filter(_.id === id).result.headOption)
}
