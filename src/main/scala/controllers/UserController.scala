package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import models.{User, UserJson}
import repositories.UserRepository
import akka.http.scaladsl.server.Directives._

class UserController(val userRepository: UserRepository) extends UserJson {
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
          get {
            onSuccess(userRepository.findById(id)) {
              case Some(user) => complete(StatusCodes.OK, user)
              case None => complete(StatusCodes.NotFound)
            }
          }
        }
      }
  }
}
