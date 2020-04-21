package repositories

import models.Category
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import services.{ConfigService, FlywayService, PostgresService}

class CategoryRepositorySpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll
  with ConfigService {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)

  override def beforeAll(): Unit = flywayService.migrateDatabase

  override def afterAll(): Unit = flywayService.dropDatabase

  "A CategoryRepository" should {
    "be empty at the beginning" in {
      categoryRepository.all.map { categories => categories.size shouldBe 0 }
    }

    "create valid categories" in {
      categoryRepository.create(Category(None, "First test category")).flatMap { category =>
        category.id shouldBe defined
      }
      categoryRepository.all.map { categories => categories.size shouldBe 1 }
    }

    "not find a category by title if it doesn't exist" in {
      categoryRepository.findByTitle("not a valid title").map { category => category should not be defined }
    }

    "find a category by title if it exist" in {
      categoryRepository.create(Category(None, "Second test category")).flatMap { category =>
        categoryRepository.findByTitle(category.title).map { c =>
          c shouldBe defined
        }
      }
    }

    "delete a category by id if exist" in {
      categoryRepository.create(Category(None, "Third test category")).flatMap { category =>
        categoryRepository.delete(category.id.get).flatMap { _ =>
          categoryRepository.findByTitle(category.title).map { c => c should not be defined }
        }
      }
    }
  }
}
