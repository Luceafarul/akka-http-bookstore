package controllers

import java.time.LocalDate
import java.time.format.DateTimeParseException

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives.{complete, formFields, get, pathEndOrSingleSlash, pathPrefix, post, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{PredefinedFromStringUnmarshallers, Unmarshaller}
import models.Book
import models.search.BookSearch
import repositories.{BookRepository, CategoryRepository}
import services.CurrencyService
import views.BookSearchView

import scala.concurrent.{ExecutionContext, Future}

class BookSearchController(val bookRepository: BookRepository, val categoryRepository: CategoryRepository)
                          (implicit val executionContext: ExecutionContext, val system: ActorSystem)
  extends PredefinedFromStringUnmarshallers {

  implicit val localDateFromStringUnmarshaller: Unmarshaller[String, Either[String, Option[LocalDate]]] =
    Unmarshaller.strict[String, Either[String, Option[LocalDate]]] {
      case s if s.isEmpty => Right(None)
      case s => try {
        Right(Some(LocalDate.parse(s)))
      } catch {
        case _: DateTimeParseException => Left("Invalid date format, please enter a valid one. YYYY-MM-DD")
      }
    }

  val routes: Route = pathPrefix("books") {
    pathEndOrSingleSlash {
      get {
        complete {
          responseWithView(bookRepository.all)
        }
      } ~
        post {
          formFields(("title".?, "releaseDate".as[Either[String, Option[LocalDate]]], "author".?, "categoryId".as[Long].?, "currency")) {
            (title, releaseDate, author, categoryId, currency) =>
              complete {
                releaseDate match {
                  case Right(value) =>
                    val bookSearch = BookSearch(title, value, categoryId, author)

                    val booksFutureQuery = bookRepository.search(bookSearch)

                    val booksFuture = if (currency != CurrencyService.baseCurrency) {
                      booksFutureQuery.flatMap { books =>
                        CurrencyService.rates.map { someRates =>
                          someRates.fold(books) { rates =>
                            rates.get(currency).fold(books) { rate =>
                              books.map(book => book.copy(price = book.price * rate))
                            }
                          }
                        }
                      }
                    } else booksFutureQuery

                    responseWithView(booksFuture, currency)

                  case Left(value) =>
                    responseWithView(bookRepository.all, exceptions = List(value))
                }
              }
          }
        }
    }
  }

  private def responseWithView(booksFuture: Future[Seq[Book]],
                               currency: String = CurrencyService.baseCurrency,
                               exceptions: List[String] = List.empty): Future[HttpResponse] = {
    val currencies = CurrencyService.supportedCurrencies
    for {
      categories <- categoryRepository.all
      books <- booksFuture
    } yield {
      val view = BookSearchView.view(categories, currencies, books, currency, exceptions)
      HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, view))
    }
  }
}
