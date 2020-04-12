package services

import com.typesafe.config.ConfigFactory

trait ConfigService {
  private val config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")
  private val dbConfig = config.getConfig("database")

  val httpHost = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")

  val dbUrl = dbConfig.getString("url")
  val dbUser = dbConfig.getString("user")
  val dbPassword = dbConfig.getString("password")
}
