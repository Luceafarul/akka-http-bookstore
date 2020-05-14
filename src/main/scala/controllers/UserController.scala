package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import models.{User, UserJson}
import repositories.UserRepository
import akka.http.scaladsl.server.Directives._
import services.TokenService
import web.directives.VerifyToken

import scala.concurrent.ExecutionContext

class UserController(val userRepository: UserRepository, val tokenService: TokenService)
                    (implicit val executor: ExecutionContext) extends UserJson with VerifyToken {

  val routes: Route = pathPrefix("users") {
    pathEndOrSingleSlash {
      post {
        entity(as[User]) { user =>
          onSuccess(userRepository.findByEmail(user.email)) {
            case Some(_) => complete(StatusCodes.BadRequest, "User with this email already exist.")
            case None => complete(StatusCodes.Created, userRepository.create(user))
          }
        }
      }
    } ~
      pathPrefix(LongNumber) { id =>
        pathEndOrSingleSlash {
          verifyTokenUser(id) { user =>
            get {
              complete(user)
            }
          }
        }
      }
  }
}
