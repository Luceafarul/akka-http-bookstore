package web.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import models.User
import services.TokenService

import scala.concurrent.ExecutionContext

trait VerifyToken {
  val tokenService: TokenService

  implicit val executor: ExecutionContext

  def verifyToken: Directive1[User] = {
    headerValueByName("Authorization").flatMap { token =>
      onSuccess(tokenService.fetchUser(token)).flatMap {
        case Some(user) => provide(user)
        case None => complete(StatusCodes.Unauthorized)
      }
    }
  }

  def verifyTokenUser(userId: Long): Directive1[User] = verifyToken.flatMap { userInToken =>
    userInToken.id match {
      case Some(id) =>
        if (userId == id) provide(userInToken)
        else complete(StatusCodes.Unauthorized)
      case None => complete(StatusCodes.Unauthorized)
    }
  }
}
