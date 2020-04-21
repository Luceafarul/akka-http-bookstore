package models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import spray.json.DefaultJsonProtocol

case class Category(id: Option[Long] = None, title: String)

trait CategoryJson extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val categoryFormat = jsonFormat2(Category.apply)
}

trait CategoryTable {
  class Categories(tag: Tag) extends Table[Category](tag, "categories") {
    def id: Rep[Option[Long]] = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column[String]("title")

    override def * : ProvenShape[Category] = (id, title) <> ((Category.apply _).tupled, Category.unapply)
  }

  protected val categories = TableQuery[Categories]
}
