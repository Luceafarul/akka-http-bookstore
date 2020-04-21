package services

import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

trait DatabaseService {
  def driver: JdbcProfile
  def db: Database
}
