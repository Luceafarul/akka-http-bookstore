package web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import repositories.{AuthRepository, BookRepository, CategoryRepository, OrderRepository, UserRepository}
import services.{ApiService, ConfigService, DatabaseService, FlywayService, PostgresService, TokenService}

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object WebServer extends App
  with ConfigService
  with WebApi {
  override implicit def system: ActorSystem = ActorSystem("akka-http-bookstore")

  override implicit def materializer: Materializer = Materializer.matFromSystem

  override implicit def executor: ExecutionContext = system.dispatcher

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  flywayService.migrateDatabase

  val databaseService: DatabaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)
  val orderRepository = new OrderRepository(databaseService)
  val bookRepository = new BookRepository(databaseService)
  val authRepository = new AuthRepository(databaseService)
  val userRepository = new UserRepository(databaseService)

  val tokenService = new TokenService(userRepository)

  val apiService = new ApiService(
    categoryRepository,
    bookRepository,
    authRepository,
    userRepository,
    orderRepository,
    tokenService
  )

  val bindingFuture = Http().bindAndHandle(apiService.routes, httpHost, httpPort)

  println(s"Server online at $httpHost:$httpPort/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(binding => binding.unbind())
    .onComplete(_ => system.terminate())
}
