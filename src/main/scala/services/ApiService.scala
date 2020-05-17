package services

import java.time.LocalDate

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{PredefinedFromStringUnmarshallers, Unmarshaller}
import controllers.{AuthController, BookController, CategoryController, UserController}
import models.BookJson
import models.search.BookSearch
import repositories.{AuthRepository, BookRepository, CategoryRepository, UserRepository}
import views.BookSearchView

import scala.concurrent.ExecutionContext

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
          onSuccess(categoryRepository.all) { category =>
            complete {
              HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, BookSearchView.view(category, List("USD"))))
            }
          }
        } ~
          post {
            formFields(("title".?, "releaseDate".as[Option[LocalDate]], "author".?, "categoryId".as[Long].?, "currency".?)) {
              (title, releaseDate, author, categoryId, _) =>
                val bookSearch = BookSearch(title, releaseDate, categoryId, author)
                complete(bookRepository.search(bookSearch))
            }
          }
      }
    }
}
