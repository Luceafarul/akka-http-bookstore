package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import models.{User, UserJson}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.UserRepository
import services.{ConfigService, FlywayService, PostgresService}
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

  val userController = new UserController(userRepository)

  val testUser = User(None, "Marcus", "ma@test.mail", "Ma1234567890")

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

    "return NotFound when no user is found by id" in {
      val notExistedId = 0
      Get(s"/users/$notExistedId") ~> userController.routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return the user data when it is found by id" in {
      for {
        user <- userRepository.create(testUser)
        result <- Get(s"/users/${user.id.getOrElse(0)}") ~> userController.routes ~> check {
          status shouldBe StatusCodes.OK
          val foundUser = responseAs[User]
          foundUser.email shouldBe user.email
        }
        _ <- userRepository.delete(user.id.get)
      } yield result
    }
  }
}
