package services

import org.flywaydb.core.Flyway

class FlywayService(dbUrl: String, dbUser: String, dbPassword: String) {
  private val flyway = new Flyway(Flyway.configure().dataSource(dbUrl, dbUser, dbPassword))

  def migrateDatabase: FlywayService = {
    flyway.migrate()
    this
  }

  def dropDatabase: FlywayService = {
    flyway.clean()
    this
  }
}
