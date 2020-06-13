package repositories

import java.sql.Timestamp
import java.time.{Instant, LocalDate}
import java.util.UUID

import models._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import services.{ConfigService, FlywayService, PostgresService}

import scala.concurrent.Future

class OrderRepositorySpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfter
  with ConfigService {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)
  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)
  val bookRepository = new BookRepository(databaseService)
  val userRepository = new UserRepository(databaseService)
  val orderRepository = new OrderRepository(databaseService)

  def testUser = User(None, "Marcus", UUID.randomUUID().toString, "password")

  val sciFiCategory = Category(None, "Sci-Fi")
  val techCategory = Category(None, "Technical")

  val bookFields = List(
    ("Akka in Action", techCategory.title, LocalDate.of(2016, 9, 1), "Raymond Roestenburg, Rob Bakker, Rob Williams"),
    ("Scala in Depth", techCategory.title, LocalDate.of(1993, 1, 1), "Joshua D. Suereth"),
    ("Code Complete", techCategory.title, LocalDate.of(1895, 1, 1), "Steve McConnell"),
    ("The Time Machine", sciFiCategory.title, LocalDate.of(1895, 1, 1), "H.G. Wells"),
    ("Nineteen Eighty-Four", sciFiCategory.title, LocalDate.of(1949, 1, 1), "George Orwell")
  )

  def testBook(categoryId: Long,
               name: String = "Foundation",
               releaseDate: LocalDate = LocalDate.of(1951, 7, 17),
               author: String = "Isaac Asimov",
               price: Double = 12.99,
               quantity: Int = 17): Book = Book(None, name, releaseDate, categoryId, quantity, price, author)

  def bulkInsert: Future[Seq[Book]] = for {
    sciFi <- categoryRepository.create(sciFiCategory)
    tech <- categoryRepository.create(techCategory)
    book <- Future.sequence(bookFields.map { case (name, categoryId, releaseDate, author) =>
      val cId = if (categoryId == sciFiCategory.title) sciFi.id.get else tech.id.get
      val b = testBook(cId, name, releaseDate, author)
      bookRepository.create(b)
    })
  } yield book

  before {
    flywayService.migrateDatabase
  }

  after {
    flywayService.dropDatabase
  }

  "An Order Repository" should {
    "create orders" in {
      for {
        user <- userRepository.create(testUser)
        books <- bulkInsert
        totalPrice = books.map(_.price).sum
        order = Order(None, Timestamp.from(Instant.now()), user.id.get, totalPrice)
        orderWithBooks = OrderWithBooks(order, books)
        createdOrder <- orderRepository.createOrder(orderWithBooks)
      } yield {
        createdOrder.id shouldBe defined
      }
    }

    "find orders by user" in {
      for {
        user <- userRepository.create(testUser)
        books <- bulkInsert
        totalPrice = books.map(_.price).sum
        order = Order(None, Timestamp.from(Instant.now()), user.id.get, totalPrice)
        orderWithBooks = OrderWithBooks(order, books)
        createdOrder <- orderRepository.createOrder(orderWithBooks)
        orders <- orderRepository.findOrdersByUser(user.id.get)
      } yield {
        orders.size shouldBe 1
        orders.head.id shouldBe createdOrder.id
      }
    }

    "find books by order" in {
      for {
        user <- userRepository.create(testUser)
        books <- bulkInsert
        totalPrice = books.map(_.price).sum
        order = Order(None, Timestamp.from(Instant.now()), user.id.get, totalPrice)
        orderWithBooks = OrderWithBooks(order, books)
        createdOrder <- orderRepository.createOrder(orderWithBooks)
        booksByOrder <- orderRepository.findBooksByOrder(createdOrder.id.get)
      } yield {
        booksByOrder.size shouldBe books.size
        booksByOrder.map(_.bookId).sorted shouldBe books.map(_.id.getOrElse(0L)).sorted
      }
    }

    "filters properly" in {
      for {
        user <- userRepository.create(testUser)
        books <- bulkInsert
        totalPrice = books.map(_.price).sum
        order = Order(None, Timestamp.from(Instant.now()), user.id.get, totalPrice)
        orderWithBooks = OrderWithBooks(order, books)
        __ <- orderRepository.createOrder(orderWithBooks)
        orders <- orderRepository.findOrdersByUser(-7L)
        storedBooks <- orderRepository.findBooksByOrder(-13L)
      } yield {
        orders shouldBe empty
        storedBooks shouldBe empty
      }
    }
  }
}
