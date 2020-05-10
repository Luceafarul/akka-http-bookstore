package repositories

import models.{Credentials, User}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import services.{ConfigService, FlywayService, PostgresService}

class AuthRepositorySpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll
  with ConfigService {

  val flywayService = new FlywayService(dbUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(dbUrl, dbUser, dbPassword)

  val userRepository = new UserRepository(databaseService)

  val authRepository = new AuthRepository(databaseService)

  val testUser = User(None, "Marcus", "ma@test.mail", "Ma1234567890")

  override def beforeAll(): Unit = flywayService.migrateDatabase

  override def afterAll(): Unit = flywayService.dropDatabase

  "An AuthRepository" should {
    "return None when there are no users" in {
      for {
        foundUser <- authRepository.findByCredentials(Credentials(testUser.email, testUser.password))
      } yield {
        foundUser should not be defined
      }
    }

    "return None when the Credentials does not match correctly" in {
      for {
        user <- userRepository.create(testUser)
        foundUser <- authRepository.findByCredentials(Credentials(testUser.email, "wrong password"))
        _ <- userRepository.delete(user.id.get)
      } yield {
        foundUser should not be defined
      }
    }

    "return the user when the Credentials matches correctly" in {
      import com.github.t3hnar.bcrypt._

      for {
        user <- userRepository.create(testUser)
        Some(foundUser) <- authRepository.findByCredentials(Credentials(testUser.email, testUser.password))
        _ <- userRepository.delete(user.id.get)
      } yield {
        foundUser.email shouldBe testUser.email
        testUser.password.isBcrypted(foundUser.password) shouldBe true
      }
    }
  }
}
