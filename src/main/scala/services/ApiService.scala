package services

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{PredefinedFromStringUnmarshallers, Unmarshaller}
import akka.stream.Materializer
import controllers.{AuthController, BookController, CategoryController, UserController}
import models.search.BookSearch
import models.{Book, BookJson}
import repositories.{AuthRepository, BookRepository, CategoryRepository, UserRepository}
import views.BookSearchView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ApiService(
                  categoryRepository: CategoryRepository,
                  bookRepository: BookRepository,
                  authRepository: AuthRepository,
                  userRepository: UserRepository,
                  tokenService: TokenService)
                (
                  implicit executor: ExecutionContext,
                  as: ActorSystem,
                  mat: Materializer) extends BookJson
  with PredefinedFromStringUnmarshallers {

  implicit val localDateFromStringUnmarshaller: Unmarshaller[String, Option[LocalDate]] =
    Unmarshaller.strict[String, Option[LocalDate]] { dateFromString =>
      Try(LocalDate.parse(dateFromString)) match {
        case Success(value) => Some(value)
        case Failure(_) => None
      }
    }

  val categoryController = new CategoryController(categoryRepository, tokenService)
  val bookController = new BookController(bookRepository)
  val userController = new UserController(userRepository, tokenService)
  val authController = new AuthController(authRepository, tokenService)

  def routes: Route = pathPrefix("api") {
    authController.routes ~
      userController.routes ~
      categoryController.routes ~
      bookController.routes
  } ~
    pathPrefix("books") {
      pathEndOrSingleSlash {
        get {
          complete {
            responseWithView(bookRepository.all)
          }
        } ~
          post {
            formFields(("title".?, "releaseDate".as[Option[LocalDate]], "author".?, "categoryId".as[Long].?, "currency")) {
              (title, releaseDate, author, categoryId, currency) =>
                val bookSearch = BookSearch(title, releaseDate, categoryId, author)
                complete {
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
                }
            }
          }
      }
    }

  private def responseWithView(booksFuture: Future[Seq[Book]],
                               currency: String = CurrencyService.baseCurrency): Future[HttpResponse] = {
    val currencies = CurrencyService.supportedCurrencies
    for {
      categories <- categoryRepository.all
      books <- booksFuture
    } yield {
      val view = BookSearchView.view(categories, currencies, books, currency)
      HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, view))
    }
  }
}
