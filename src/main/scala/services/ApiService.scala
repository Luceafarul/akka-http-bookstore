package services

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import controllers.{AuthController, BookController, CategoryController, OrderController, UserController}
import repositories.{AuthRepository, BookRepository, CategoryRepository, OrderRepository, UserRepository}

import scala.concurrent.ExecutionContext

class ApiService(
                  categoryRepository: CategoryRepository,
                  bookRepository: BookRepository,
                  authRepository: AuthRepository,
                  userRepository: UserRepository,
                  orderRepository: OrderRepository,
                  tokenService: TokenService)(implicit executor: ExecutionContext) {

  val categoryController = new CategoryController(categoryRepository, tokenService)
  val bookController = new BookController(bookRepository)
  val orderController = new OrderController(orderRepository, tokenService)
  val userController = new UserController(userRepository, tokenService)
  val authController = new AuthController(authRepository, tokenService)

  def routes: Route = pathPrefix("api") {
    authController.routes ~
      userController.routes ~
      categoryController.routes ~
      bookController.routes ~
      orderController.routes
  }
}
