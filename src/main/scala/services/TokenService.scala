package services

import models.{User, UserJson}
import pdi.jwt.{Jwt, JwtAlgorithm}
import repositories.UserRepository
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TokenService(val userRepository: UserRepository)(implicit executor: ExecutionContext) extends UserJson
  with ConfigService {

  // TODO extract it from config service???
  private val tempKey = "super-secret-ket"

  // TODO how to encode email and id?
  def createToken(user: User): String =
    Jwt.encode(user.email.toJson.toString, tempKey, JwtAlgorithm.HS256)

  def isTokenValid(token: String): Boolean =
    Jwt.isValid(token, tempKey, Seq(JwtAlgorithm.HS256))

  def fetchUser(token: String): Future[Option[User]] = {
    Jwt.decodeRaw(token, tempKey, Seq(JwtAlgorithm.HS256)) match {
      case Success(json) =>
        val email = json.parseJson.convertTo[String]
        userRepository.findByEmail(email)
      case Failure(exception) => Future.failed(exception)
    }
  }

  def isTokenValidForMember(token: String, user: User): Future[Boolean] = fetchUser(token).map {
    case Some(fetchedUser) => fetchedUser.email == user.email
    case None => false
  }
}
