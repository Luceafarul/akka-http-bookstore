package controllers

import java.time.LocalDate

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import models.{Book, BookJson, Category}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.{BookRepository, CategoryRepository}
import services.{ConfigService, FlywayService, PostgresService}
import web.WebApi

import scala.concurrent.Future

class BookEndpointSpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with BookJson {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  val postgresService = new PostgresService(dbUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(postgresService)

  val bookRepository = new BookRepository(postgresService)

  val bookController = new BookController(bookRepository)

  override protected def beforeAll(): Unit = {
    flywayService.migrateDatabase

    for {
      sciFi <- categoryRepository.create(sciFiCategory)
      tech <- categoryRepository.create(techCategory)
      book <- Future.sequence(bookFields.map { case (name, categoryId, releaseDate, author) =>
        val cId = if (categoryId == sciFiCategory.title) sciFi.id.get else tech.id.get
        val b = testBook(cId, name, releaseDate, author)
        bookRepository.create(b)
      })
    } yield book
  }

  override protected def afterAll(): Unit = {
    for {
      books <- bookRepository.all
      _ <- Future.sequence(books.map(book => bookRepository.delete(book.id.get)))
      _ <- categoryRepository.delete(sciFiCategory.id.get)
      _ <- categoryRepository.delete(techCategory.id.get)
    } yield books

    flywayService.dropDatabase
  }

  def testBook(categoryId: Long,
               name: String = "Foundation",
               releaseDate: LocalDate = LocalDate.of(1951, 7, 17),
               author: String = "Isaac Asimov",
               quantity: Int = 17): Book = Book(None, name, releaseDate, categoryId, quantity, author)

  val sciFiCategory = Category(None, "Sci-Fi")
  val techCategory = Category(None, "Technical")

  val bookFields = List(
    ("Akka in Action", techCategory.title, LocalDate.of(2016, 9, 1), "Raymond Roestenburg, Rob Bakker, Rob Williams"),
    ("Scala in Depth", techCategory.title, LocalDate.of(1993, 1, 1), "Joshua D. Suereth"),
    ("Code Complete", techCategory.title, LocalDate.of(1895, 1, 1), "Steve McConnell"),
    ("The Time Machine", sciFiCategory.title, LocalDate.of(1895, 1, 1), "H.G. Wells"),
    ("Nineteen Eighty-Four", sciFiCategory.title, LocalDate.of(1949, 1, 1), "George Orwell")
  )

  "A Book Controller" should {
    "create a book" in {
      categoryRepository.create(Category(None, "Test category")).flatMap { category =>
        Post("/books/", testBook(category.id.get)) ~> bookController.routes ~> check {
          status shouldBe StatusCodes.Created
          val book = responseAs[Book]

          for {
            _ <- bookRepository.delete(book.id.get)
            _ <- categoryRepository.delete(category.id.get)
          } yield {
            book.id shouldBe defined
            book.title shouldBe "Foundation"
          }
        }
      }
    }

    "return NotFound when try to delete not existent book" in {
      Delete("/books/777") ~> bookController.routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return NoContent when delete existent book" in {
      categoryRepository.create(Category(None, "Test category")).flatMap { category =>
        bookRepository.create(testBook(category.id.get)).flatMap { book =>
          Delete(s"/books/${book.id.get}") ~> bookController.routes ~> check {
            categoryRepository.delete(category.id.get)

            status shouldBe StatusCodes.NoContent
          }
        }
      }
    }

    "return all books when no query parameters are sent" in {
      Get("/books/") ~> bookController.routes ~> check {
        status shouldBe StatusCodes.OK
        val books = responseAs[List[Book]]

        books should have size bookFields.size
      }
    }

    "return all books that conform to the parameters sent" in {
      Get("/books?title=in") ~> bookController.routes ~> check {
        status shouldBe StatusCodes.OK
        val books = responseAs[List[Book]]

        books should have size 4
        books.map(_.title) should contain allOf("Akka in Action", "Scala in Depth", "The Time Machine", "Nineteen Eighty-Four")
      }

      Get("/books?title=in&author=Ray") ~> bookController.routes ~> check {
        status shouldBe StatusCodes.OK
        val books = responseAs[List[Book]]

        books should have size 1
        books.head.title shouldBe "Akka in Action"
      }
    }
  }
}
