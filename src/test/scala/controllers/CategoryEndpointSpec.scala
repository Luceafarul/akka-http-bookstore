package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingHeaderRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import helpers.CategorySpecHelper
import models.{Category, CategoryJson, User}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.{CategoryRepository, UserRepository}
import services.{ConfigService, FlywayService, PostgresService, TokenService}
import web.WebApi

class CategoryEndpointSpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with CategoryJson {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  override protected def beforeAll(): Unit = flywayService.migrateDatabase

  override protected def afterAll(): Unit = flywayService.dropDatabase

  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)
  val categoryRepository = new CategoryRepository(databaseService)
  val userRepository = new UserRepository(databaseService)

  val tokenService = new TokenService(userRepository)

  val categoryController = new CategoryController(categoryRepository, tokenService)
  val categorySpecHelper = new CategorySpecHelper(categoryRepository)

  val categoriesPath = "/categories"

  "A CategoryController" should {
    "return an empty list at the beginning" in {
      Get(categoriesPath) ~> categoryController.routes ~> check {
        status shouldBe StatusCodes.OK
        val categories = responseAs[List[Category]]
        categories should have size 0
      }
    }

    "return all the categories when there is at least one" in {
      categorySpecHelper.createAndDelete() { c =>
        Get(categoriesPath) ~> categoryController.routes ~> check {
          status shouldBe StatusCodes.OK
          val categories = responseAs[List[Category]]
          categories should (contain(c) and have size 1)
        }
      }
    }

    "return BadRequest with repeated titles" in {
      categorySpecHelper.createAndDelete() { c =>
        Post(categoriesPath, c.copy(id = None)) ~> categoryController.routes ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "crate a valid category" in {
      Post(categoriesPath, categorySpecHelper.category) ~> categoryController.routes ~> check {
        status shouldBe StatusCodes.Created
        val category = responseAs[Category]
        categoryRepository.delete(category.id.get)

        category.id shouldBe defined
        category.title shouldBe categorySpecHelper.category.title
      }
    }

    "return Unauthorized when user try delete category without token an existent category" in {
      val testUser = User(None, "Marcus", "ma@test.mail", "Ma1234567890")
      val invalidToken = tokenService.createToken(testUser)

      Delete(s"$categoriesPath/1234567890") ~>
        addHeader("Authorization", invalidToken) ~> categoryController.routes ~> check {

        status shouldBe StatusCodes.Unauthorized
      }
    }

    "reject request when try to delete a category without token" in {
      Delete(s"$categoriesPath/1234567890") ~> categoryController.routes ~> check {
        rejection shouldBe MissingHeaderRejection("Authorization")
      }
    }

    "return NotFound when try to delete a non existent category" in {
      val testUser = User(None, "Marcus", "ma@test.mail", "Ma1234567890")
      val token = tokenService.createToken(testUser)

      for {
        user <- userRepository.create(testUser)
        result <- Delete(s"$categoriesPath/1234567890") ~> addHeader("Authorization", token) ~> categoryController.routes ~> check {
          status shouldBe StatusCodes.NotFound
        }
        _ <- userRepository.delete(user.id.get)
      } yield result
    }

    "return NoContent when delete an existent category" in {
      val testUser = User(None, "Marcus", "ma@test.mail", "Ma1234567890")
      val token = tokenService.createToken(testUser)

      for {
        user <- userRepository.create(testUser)
        category <- categoryRepository.create(categorySpecHelper.category)
        result <- Delete(s"$categoriesPath/${category.id.get}") ~> addHeader("Authorization", token)~> categoryController.routes ~> check {
          status shouldBe StatusCodes.NoContent

          categoryRepository.findByTitle(category.title).flatMap { cat =>
            cat should not be defined
          }
        }
        _ <- userRepository.delete(user.id.get)
      } yield result
    }
  }
}
