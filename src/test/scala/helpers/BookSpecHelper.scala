package helpers

import java.time.LocalDate

import models.{Book, Category}
import repositories.{BookRepository, CategoryRepository}

import scala.concurrent.{ExecutionContext, Future}

class BookSpecHelper(categoryRepository: CategoryRepository)(bookRepository: BookRepository)(implicit executor: ExecutionContext) {
  val category = Category(None, "Sci-Fi")
  def book(categoryId: Long) = Book(None, "Murder in Ganymede", LocalDate.of(1998, 1, 20), categoryId, 3, "John Doe")

  // Helper function to create a category, then a book, perform some assertions to it and then delete them both
  def createAndDelete[T]()(assertion: Book => Future[T]): Future[T] = ???
}
