package services

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import controllers.{BookController, CategoryController}
import repositories.{BookRepository, CategoryRepository}

import scala.concurrent.ExecutionContext

class ApiService(categoryRepository: CategoryRepository, bookRepository: BookRepository, tokenService: TokenService)
                (implicit executor: ExecutionContext) {

  val categoryController = new CategoryController(categoryRepository, tokenService)
  val bookController = new BookController(bookRepository)

  def routes: Route = pathPrefix("api") {
    categoryController.routes ~
      bookController.routes
  }
}
