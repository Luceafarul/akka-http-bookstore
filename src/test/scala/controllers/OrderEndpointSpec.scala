package controllers

import java.sql.Timestamp
import java.time.{Instant, LocalDate}

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import models.{Book, BookByOrder, Category, Order, OrderJson, OrderWithBooks, User}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.{BookRepository, CategoryRepository, OrderRepository, UserRepository}
import services.{ConfigService, FlywayService, PostgresService, TokenService}
import web.WebApi

import scala.concurrent.Future

class OrderEndpointSpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfter
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with OrderJson {

  override implicit val executor = system.dispatcher

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)
  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val orderRepository = new OrderRepository(databaseService)
  val userRepository = new UserRepository(databaseService)
  val categoryRepository = new CategoryRepository(databaseService)
  val bookRepository = new BookRepository(databaseService)

  val tokenService = new TokenService(userRepository)

  val orderController = new OrderController(orderRepository, tokenService)

  def testUser = User(None, "Marcus", "marcus@aur.test", "password")

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

  "An Order Controller" should {
    "should create an order" in {
      for {
        user <- userRepository.create(testUser)
        token = tokenService.createToken(user)
        books <- bulkInsert
        totalPrice = books.map(_.price).sum
        order = Order(None, Timestamp.from(Instant.now()), user.id.get, totalPrice)
        orderWithBooks = OrderWithBooks(order, books)
      } yield Post("/orders", orderWithBooks) ~> addHeader("Authorization", token) ~> orderController.routes ~> check {
        status shouldBe StatusCodes.Created
      }
    }

    "should find order by user id" in {
      for {
        user <- userRepository.create(testUser)
        token = tokenService.createToken(user)
        books <- bulkInsert
        totalPrice = books.map(_.price).sum
        order = Order(None, Timestamp.from(Instant.now()), user.id.get, totalPrice)
        orderWithBooks = OrderWithBooks(order, books)
        createdOrder <- orderRepository.createOrder(orderWithBooks)
      } yield Get("/orders") ~> addHeader("Authorization", token) ~> orderController.routes ~> check {
        status shouldBe StatusCodes.OK
        val resp = responseAs[Seq[Order]]
        resp shouldBe Seq(createdOrder)
      }
    }

    "should find books by order" in {
      for {
        user <- userRepository.create(testUser)
        token = tokenService.createToken(user)
        books <- bulkInsert
        totalPrice = books.map(_.price).sum
        order = Order(None, Timestamp.from(Instant.now()), user.id.get, totalPrice)
        orderWithBooks = OrderWithBooks(order, books)
        createdOrder <- orderRepository.createOrder(orderWithBooks)
      } yield Get(s"/orders/${createdOrder.id.get}/books") ~> addHeader("Authorization", token) ~> orderController.routes ~> check {
        status shouldBe StatusCodes.OK
        val resp = responseAs[Seq[BookByOrder]]
        resp.size shouldBe books.size
        resp.map(_.bookId) should contain theSameElementsAs books.flatMap(_.id)
      }
    }
  }
}
