package repositories

import models.{Credentials, User, UserTable}
import services.DatabaseService

import scala.concurrent.{ExecutionContext, Future}

class AuthRepository(val databaseService: DatabaseService)(implicit executor: ExecutionContext)
  extends UserTable {

  import databaseService._
  import slick.jdbc.PostgresProfile.api._
  import com.github.t3hnar.bcrypt._

  def findByCredentials(credentials: Credentials): Future[Option[User]] = {
    db.run(users.filter(_.email === credentials.email).result.headOption).map {
      case result @ Some(user) =>
        if (credentials.password.isBcrypted(user.password)) result
        else None
      case None => None
    }
  }
}
