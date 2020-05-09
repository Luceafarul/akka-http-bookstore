package repositories

import models.User
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import services.{ConfigService, FlywayService, PostgresService}
import com.github.t3hnar.bcrypt._

class UserRepositorySpec extends AsyncWordSpec
with Matchers
with BeforeAndAfterAll
with ConfigService {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val userRepository = new UserRepository(databaseService)

  val testUser = User(None, "Marcus", "ma@test.mail", "Ma1234567890")

  override def beforeAll(): Unit = flywayService.migrateDatabase

  override def afterAll(): Unit = flywayService.dropDatabase

  "A UserRepository" should {
    "be empty at the beginning" in {
      userRepository.all.map { users => users should have size 0 }
    }

    "create a valid user" in {
      for {
        user <- userRepository.create(testUser)
        _ <- userRepository.delete(user.id.get)
      } yield {
        user.id shouldBe defined
        testUser.password.isBcrypted(user.password) shouldBe true
      }
    }

    "not find a user by if ti doesn't exist" in {
      userRepository.findByEmail("test@test.mail").map { user => user should not be defined }
    }

    "find a user by email if it exists" in {
      for {
        user <- userRepository.create(testUser)
        Some(foundUser) <- userRepository.findByEmail(testUser.email)
        _ <- userRepository.delete(user.id.get)
      } yield {
        user.id shouldBe foundUser.id
      }
    }

    "not find a user by id if it doesn't exist" in {
      val notExistedId = 0
      userRepository.findById(notExistedId).map { user => user should not be defined }
    }

    "find a user by id if it exists" in {
      for {
        user <- userRepository.create(testUser)
        Some(foundUser) <- userRepository.findById(user.id.get)
        _ <- userRepository.delete(user.id.get)
      } yield {
        user.id shouldBe foundUser.id
      }
    }

    "delete a user by id if it exists" in {
      for {
        user <- userRepository.create(testUser)
        _ <- userRepository.delete(user.id.get)
        users <- userRepository.all
      } yield {
        users shouldBe empty
      }
    }
  }
}
