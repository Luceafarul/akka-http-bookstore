package repositories

import java.time.LocalDate

import models.search.BookSearch
import models.{Book, Category}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import services.{ConfigService, FlywayService, PostgresService}

import scala.concurrent.Future

class BookSearchSpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll
  with ConfigService {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  val postgresService = new PostgresService(dbUrl, dbUser, dbPassword)

  val bookRepository = new BookRepository(postgresService)

  val categoryRepository = new CategoryRepository(postgresService)

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

  "A Search API" should {
    "return an empty list if there are no matches" in {
      val bookSearch = BookSearch(title = Some("Non exist title"))
      bookRepository.search(bookSearch).map { books =>
        books should have size 0
      }
    }

    "return the matching books by title" in {
      val bookSearch = BookSearch(title = Some("Akka"))
      bookRepository.search(bookSearch).map { books =>
        books should have size 1
        books.head.title shouldBe "Akka in Action"
      }

      val bookSearchMultiple = BookSearch(title = Some("in"))
      bookRepository.search(bookSearchMultiple).map { books =>
        books should have size 4
      }
    }

    "return the books by release date" in {
      val bookSearch = BookSearch(releaseDate = Some(LocalDate.of(1895, 1, 1)))
      bookRepository.search(bookSearch).map { books =>
        books should have size 2
        books.map(_.title) should contain allOf("The Time Machine", "Code Complete")
      }
    }

    "return the books by category" in {
      for {
        Some(category) <- categoryRepository.findByTitle(sciFiCategory.title)
        books <- bookRepository.search(BookSearch(categoryId = category.id))
      } yield {
        books should have size 2
        books.map(_.title) should contain allOf("The Time Machine", "Nineteen Eighty-Four")
      }
    }

    "return the books by author name" in {
      val bookSearch = BookSearch(author = Some("W"))
      bookRepository.search(bookSearch).map { books =>
        books should have size 2
      }
    }

    "return correctly the expect books when combine search" in {
      for {
        Some(category) <- categoryRepository.findByTitle(techCategory.title)
        bookSearch = BookSearch(title = Some("Akka"), categoryId = category.id)
        books <- bookRepository.search(bookSearch)
      } yield {
        books should have size 1
        books.head.title shouldBe "Akka in Action"
      }

      val bookSearch = BookSearch(title = Some("Scala"), author = Some("Wells"))
      bookRepository.search(bookSearch).map { books =>
        books should have size 0
      }
    }
  }
}
