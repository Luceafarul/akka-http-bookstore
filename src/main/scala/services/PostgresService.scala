package services

import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

class PostgresService(dbUrl: String, dbUser: String, dbPassword: String) extends DatabaseService {
  override val driver = PostgresProfile

  override def db: Database = Database.forURL(dbUrl, dbUser, dbPassword)

  db.createSession()
}
