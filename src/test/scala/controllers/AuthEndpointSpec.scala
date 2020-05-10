package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import models.{Auth, AuthJson, Credentials, CredentialsJson, User}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.{AuthRepository, UserRepository}
import services.{ConfigService, FlywayService, PostgresService, TokenService}
import web.WebApi

class AuthEndpointSpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with CredentialsJson
  with AuthJson {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val userRepository = new UserRepository(databaseService)

  val authRepository = new AuthRepository(databaseService)

  val tokenService = new TokenService(userRepository)

  val authController = new AuthController(authRepository, tokenService)

  val testUser = User(None, "Marcus", "ma@test.mail", "Ma1234567890")

  val testCredentials = Credentials(testUser.email, testUser.password)

  override def beforeAll(): Unit = flywayService.migrateDatabase

  override def afterAll(): Unit = flywayService.dropDatabase

  "An AuthEndpoint" should {
    "return an Auth when long is successful" in {
      for {
        user <- userRepository.create(testUser)
        assertionResult <- Post("/auth", testCredentials) ~> authController.routes ~> check {
          status shouldBe StatusCodes.OK

          val auth = responseAs[Auth]
          auth.user.email shouldBe testUser.email
          tokenService.isTokenValidForMember(auth.token, testUser).map { result => result shouldBe true }
        }
        _ <- userRepository.delete(user.id.get)
      } yield assertionResult
    }

    "return an Unauthorized status code when login fails" in {
      Post("/auth", testCredentials) ~> authController.routes ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }
  }
}
