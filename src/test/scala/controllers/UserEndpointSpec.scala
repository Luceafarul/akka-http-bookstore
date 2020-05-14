package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import models.{User, UserJson}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.UserRepository
import services.{ConfigService, FlywayService, PostgresService, TokenService}
import web.WebApi

class UserEndpointSpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with UserJson {

  override implicit val executor = system.dispatcher

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val userRepository = new UserRepository(databaseService)

  val tokenService = new TokenService(userRepository)

  val userController = new UserController(userRepository, tokenService)

  val testUser = User(None, "Marcus", "ma@test.mail", "Ma1234567890")

  val testUserToken = tokenService.createToken(testUser)

  override def beforeAll(): Unit = flywayService.migrateDatabase

  override def afterAll(): Unit = flywayService.dropDatabase

  "A User Controller" should {
    "return BadRequest with repeated emails" in {
      for {
        user <- userRepository.create(testUser)
        result <- Post("/users", testUser) ~> userController.routes ~> check {
          status shouldBe StatusCodes.BadRequest
        }
        _ <- userRepository.delete(user.id.get)
      } yield result
    }

    "create a user" in {
      for {
        result <- Post("/users", testUser) ~> userController.routes ~> check {
          status shouldBe StatusCodes.Created
          val user = responseAs[User]
          user.email shouldBe testUser.email
        }
        Some(foundUser) <- userRepository.findByEmail(testUser.email)
        _ <- userRepository.delete(foundUser.id.get)
      } yield result
    }

    "return Unauthorized when try to get no existing user by id" in {
      val invalidUser = User(Some(1234567890), "Marcus", "ma@test.mail", "Ma1234567890")
      val invalidToken = tokenService.createToken(invalidUser)
      val notExistedId = 0
      Get(s"/users/$notExistedId") ~> addHeader("Authorization", invalidToken) ~> userController.routes ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return Unauthorized when one user try to get by id second one user" in {
      val testUser2 = User(None, "Lucian", "lcn@test.mail", "Lcn1234567890")

      for {
        user1 <- userRepository.create(testUser)
        user2 <- userRepository.create(testUser2)
        assertionResult <- Get(s"/users/${user2.id.get}") ~> addHeader("Authorization", testUserToken) ~> userController.routes ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
        _ <- userRepository.delete(user1.id.get)
        _ <- userRepository.delete(user2.id.get)
      } yield assertionResult
    }

    "return the user data when it is found by id" in {
      for {
        user <- userRepository.create(testUser)
        result <- Get(s"/users/${user.id.getOrElse(0)}") ~> addHeader("Authorization", testUserToken) ~> userController.routes ~> check {
          status shouldBe StatusCodes.OK
          val foundUser = responseAs[User]
          foundUser.email shouldBe user.email
        }
        _ <- userRepository.delete(user.id.get)
      } yield result
    }
  }
}
