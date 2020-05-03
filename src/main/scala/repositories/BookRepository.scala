package repositories

import models.search.BookSearch
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

  def search(bookSearch: BookSearch): Future[Seq[Book]] = {
    val query = books.filter { book =>
      List(
        bookSearch.title.map(t => book.title.like(s"%$t%")),
        bookSearch.releaseDate.map(rd => book.releaseDate === rd),
        bookSearch.categoryId.map(cId => book.categoryId === cId),
        bookSearch.author.map(author => book.authors.like(s"%$author%"))
      ).collect { case Some(criteria) => criteria }.reduceLeftOption(_ || _).getOrElse(true: Rep[Boolean])
    }
    db.run(query.result)
  }
}
