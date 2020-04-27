package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import helpers.CategorySpecHelper
import models.{Category, CategoryJson}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.CategoryRepository
import services.{ConfigService, FlywayService, PostgresService}
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
  val categoryController = new CategoryController(categoryRepository)
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

    "return NotFound when try to delete a non existent category" in {
      Delete(s"$categoriesPath/1234567890") ~> categoryController.routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return NoContent when delete an existent category" in {
      categoryRepository.create(categorySpecHelper.category).flatMap { c =>
        Delete(s"$categoriesPath/${c.id.get}") ~> categoryController.routes ~> check {
          status shouldBe StatusCodes.NoContent

          categoryRepository.findByTitle(c.title).flatMap { cat =>
            cat should not be defined
          }
        }
      }
    }
  }
}
