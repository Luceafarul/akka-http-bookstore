package web

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import models.{Book, Category}
import repositories.{AuthRepository, BookRepository, CategoryRepository, UserRepository}
import services.{ApiService, ConfigService, DatabaseService, FlywayService, PostgresService, TokenService}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object WebServer extends App
  with ConfigService
  with WebApi {
  override implicit def system: ActorSystem = ActorSystem("akka-http-bookstore")

  override implicit def materializer: Materializer = Materializer.matFromSystem

  override implicit def executor: ExecutionContext = system.dispatcher

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  flywayService.migrateDatabase

  def fillTestData() = {
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


  val databaseService: DatabaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)
  val bookRepository = new BookRepository(databaseService)
  val authRepository = new AuthRepository(databaseService)
  val userRepository = new UserRepository(databaseService)

  val tokenService = new TokenService(userRepository)

  fillTestData()

  val apiService = new ApiService(categoryRepository, bookRepository, authRepository, userRepository, tokenService)

  val bindingFuture = Http().bindAndHandle(apiService.routes, httpHost, httpPort)

  println(s"Server online at $httpHost:$httpPort/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(binding => binding.unbind())
    .onComplete(_ => system.terminate())
}
