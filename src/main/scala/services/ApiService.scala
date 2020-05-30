package services

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers
import akka.stream.Materializer
import controllers._
import models.BookJson
import repositories.{AuthRepository, BookRepository, CategoryRepository, UserRepository}

import scala.concurrent.ExecutionContext

class ApiService(
                  categoryRepository: CategoryRepository,
                  bookRepository: BookRepository,
                  authRepository: AuthRepository,
                  userRepository: UserRepository,
                  tokenService: TokenService)
                (
                  implicit executionContext: ExecutionContext,
                  as: ActorSystem,
                  mat: Materializer) extends BookJson
  with PredefinedFromStringUnmarshallers {

  val categoryController = new CategoryController(categoryRepository, tokenService)
  val bookController = new BookController(bookRepository)
  val userController = new UserController(userRepository, tokenService)
  val authController = new AuthController(authRepository, tokenService)

  val bookSearchController = new BookSearchController(bookRepository, categoryRepository)

  def routes: Route = pathPrefix("api") {
    authController.routes ~
      userController.routes ~
      categoryController.routes ~
      bookController.routes
  } ~ bookSearchController.routes
}
