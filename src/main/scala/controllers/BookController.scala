package controllers

import java.time.LocalDate

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{PredefinedFromStringUnmarshallers, Unmarshaller}
import models.search.BookSearch
import models.{Book, BookJson}
import repositories.BookRepository

class BookController(val bookRepository: BookRepository) extends BookJson
  with PredefinedFromStringUnmarshallers {

  implicit val localDateFromStringUnmarshaller: Unmarshaller[String, LocalDate] =
    Unmarshaller.strict[String, LocalDate] { string =>
      LocalDate.parse(string)
    }

  val routes = pathPrefix("books") {
    pathEndOrSingleSlash {
      get {
        parameters(("title".?, "releaseDate".as[LocalDate].?, "categoryId".as[Long].?, "author".?))
          .as(BookSearch) { bookSearch =>
            complete {
              bookRepository.search(bookSearch)
            }
        }
      } ~
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
