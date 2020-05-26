package services

import java.time.LocalDate

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{PredefinedFromStringUnmarshallers, Unmarshaller}
import controllers.{AuthController, BookController, CategoryController, UserController}
import models.search.BookSearch
import models.{Book, BookJson}
import repositories.{AuthRepository, BookRepository, CategoryRepository, UserRepository}
import views.BookSearchView

import scala.concurrent.{ExecutionContext, Future}

class ApiService(
                  categoryRepository: CategoryRepository,
                  bookRepository: BookRepository,
                  authRepository: AuthRepository,
                  userRepository: UserRepository,
                  tokenService: TokenService)(implicit executor: ExecutionContext) extends BookJson
  with PredefinedFromStringUnmarshallers {

  implicit val localDateFromStringUnmarshaller: Unmarshaller[String, Option[LocalDate]] =
    Unmarshaller.strict[String, Option[LocalDate]] {
      case s if s.isEmpty => None
      case s => Some(LocalDate.parse(s))
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
            formFields(("title".?, "releaseDate".as[Option[LocalDate]], "author".?, "categoryId".as[Long].?, "currency".?)) {
              (title, releaseDate, author, categoryId, _) =>
                val bookSearch = BookSearch(title, releaseDate, categoryId, author)
                complete {
                  responseWithView(bookRepository.search(bookSearch))
                }
            }
          }
      }
    }

  private def responseWithView(booksFuture: Future[Seq[Book]]): Future[HttpResponse] = {
    val currencies = List("USD", "EUR")
    for {
      categories <- categoryRepository.all
      books <- booksFuture
    } yield {
      val view = BookSearchView.view(categories, currencies, books)
      HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, view))
    }
  }
}
