package repositories

import models.{User, UserTable}
import services.DatabaseService

import scala.concurrent.{ExecutionContext, Future}

class UserRepository(val databaseService: DatabaseService)(implicit val executor: ExecutionContext) extends UserTable {
  import databaseService._
  import slick.jdbc.PostgresProfile.api._

  def all: Future[Seq[User]] = db.run(users.result)

  def create(user: User): Future[User] = {
    import com.github.t3hnar.bcrypt._
    val encryptPassword = user.password.bcrypt
    val userWithEncryptedPassword = user.copy(password = encryptPassword)
    db.run(users returning users += userWithEncryptedPassword)
  }

  def findByEmail(email: String): Future[Option[User]] = db.run(users.filter(_.email === email).result.headOption)

  def findById(id: Long): Future[Option[User]] = db.run(users.filter(_.id === id).result.headOption)

  def delete(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)
}
