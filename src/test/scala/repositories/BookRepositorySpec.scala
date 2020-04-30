package repositories

import java.time.LocalDate

import models.{Book, Category}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import services.{ConfigService, FlywayService, PostgresService}

class BookRepositorySpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll
  with ConfigService {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  override protected def beforeAll(): Unit = flywayService.migrateDatabase

  override protected def afterAll(): Unit = flywayService.dropDatabase

  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)

  val bookRepository = new BookRepository(databaseService)

  "A BookRepository" should {
    "be empty at the beginning" in {
      bookRepository.all.map { books => books.size shouldBe 0 }
    }

    "create valid book" in {
      categoryRepository.create(Category(None, "Sci-Fi")).flatMap { category =>
        val book = Book(None, "Foundation", LocalDate.of(1951, 7, 17), category.id.get, 17, "Isaac Asimov")
        bookRepository.create(book).map { book =>
          bookRepository.all.map { books => books.size shouldBe 1 }
          bookRepository.delete(book.id.get)
          categoryRepository.delete(category.id.get)

          book.id shouldBe defined
        }
      }
    }

    "not find a non-existent book" in {
      bookRepository.findById(0L).flatMap { book =>
        book should not be defined
      }
    }

    "find an existing book" in {
      categoryRepository.create(Category(None, "Sci-Fi")).flatMap { category =>
        val book = Book(None, "Foundation", LocalDate.of(1951, 7, 17), category.id.get, 17, "Isaac Asimov")
        bookRepository.create(book).flatMap { book =>
          bookRepository.findById(book.id.get).map { foundBook =>
            bookRepository.delete(book.id.get)
            categoryRepository.delete(category.id.get)

            foundBook shouldBe defined
            foundBook.get shouldBe book
          }
        }
      }
    }

    "delete a book by id if it exists" in {
      for {
        category <- categoryRepository.create(Category(None, "Sci-Fi"))
        book <- bookRepository.create(Book(None, "Foundation", LocalDate.of(1951, 7, 17), category.id.get, 17, "Isaac Asimov"))
        _ <- bookRepository.delete(book.id.get)
        books <- bookRepository.all
      } yield books should have size 0
    }
  }
}
