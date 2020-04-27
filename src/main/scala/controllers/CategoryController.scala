package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import models.{Category, CategoryJson}
import repositories.CategoryRepository

class CategoryController(val categoryRepository: CategoryRepository) extends CategoryJson {

  val routes = pathPrefix("categories") {
    pathEndOrSingleSlash {
      get {
        complete {
          categoryRepository.all
        }
      } ~
      post {
        decodeRequest {
          entity(as[Category]) { category =>
            onSuccess(categoryRepository.findByTitle(category.title)) {
              case Some(_) => complete(StatusCodes.BadRequest)
              case None => complete(StatusCodes.Created, categoryRepository.create(category))
            }
          }
        }
      }
    } ~
    pathPrefix(IntNumber) { id =>
      pathEndOrSingleSlash {
        delete {
          onSuccess(categoryRepository.delete(id)) {
            case n if n != 0  => complete(StatusCodes.NoContent)
            case _ => complete(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
