package services

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import controllers.{AuthController, BookController, CategoryController, UserController}
import repositories.{AuthRepository, BookRepository, CategoryRepository, UserRepository}

import scala.concurrent.ExecutionContext

class ApiService(
                  categoryRepository: CategoryRepository,
                  bookRepository: BookRepository,
                  authRepository: AuthRepository,
                  userRepository: UserRepository,
                  tokenService: TokenService)(implicit executor: ExecutionContext) {

  val categoryController = new CategoryController(categoryRepository, tokenService)
  val bookController = new BookController(bookRepository)
  val userController = new UserController(userRepository, tokenService)
  val authController = new AuthController(authRepository, tokenService)

  def routes: Route = pathPrefix("api") {
    authController.routes ~
      userController.routes ~
      categoryController.routes ~
      bookController.routes
  }
}
