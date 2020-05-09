package models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, TableQuery}
import spray.json.DefaultJsonProtocol

case class User(id: Option[Long] = None, name: String, email: String, password: String)

trait UserJson extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val userFormat = jsonFormat4(User.apply)
}

trait UserTable {
  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id: Rep[Option[Long]] = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column[String]("name")
    def email: Rep[String] = column[String]("email", O.Unique)
    def password: Rep[String] = column[String]("password")

    override def * : ProvenShape[User] = (id, name, email, password) <> ((User.apply _).tupled, User.unapply)
  }

  protected val users = TableQuery[Users]
}
