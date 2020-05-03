package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import models.{Book, BookJson}
import repositories.BookRepository

class BookController(val bookRepository: BookRepository) extends BookJson {
  val routes = pathPrefix("books") {
    pathEndOrSingleSlash {
      post {
        entity(as[Book]) { book =>
          onComplete(bookRepository.create(book)) { createdBook =>
            complete(StatusCodes.Created, createdBook)
          }
        }
      }
    } ~
    pathPrefix(LongNumber) { id =>
      delete {
        onSuccess(bookRepository.delete(id)) {
          case n if n > 0 => complete(StatusCodes.NoContent)
          case _ => complete(StatusCodes.NotFound)
        }
      }
    }
  }
}
