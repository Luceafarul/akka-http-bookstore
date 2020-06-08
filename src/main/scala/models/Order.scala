package models

import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import spray.json.DefaultJsonProtocol

case class Order(
                  id: Option[Long] = None,
                  orderDate: Timestamp,
                  userId: Long,
                  totalPrice: Double
                )

case class BookByOrder(
                        id: Option[Long] = None,
                        orderId: Long,
                        bookId: Long,
                        unitPrice: Double,
                        quantity: Int
                      )

case class OrderWithBooks(order: Order, books: Seq[Book])

trait OrderJson extends SprayJsonSupport with DefaultJsonProtocol with BookJson {
  import converters.JsonConverters.TimestampJsonFormat

  implicit val orderFormat = jsonFormat4(Order.apply)
  implicit val bookByOrderFormat = jsonFormat5(BookByOrder.apply)
  implicit val orderWithBooksFormat = jsonFormat2(OrderWithBooks.apply)
}

trait OrderTable {
  class Orders(tag: Tag) extends Table[Order](tag, "orders") {
    def id: Rep[Option[Long]] = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def orderDate: Rep[Timestamp] = column[Timestamp]("order_date")
    def userId: Rep[Long] = column[Long]("user_id")
    def totalPrice: Rep[Double] = column[Double]("total_price_usd")

    override def * : ProvenShape[Order] = (id, orderDate, userId, totalPrice) <> ((Order.apply _).tupled, Order.unapply)
  }

  protected val orders = TableQuery[Orders]
}

trait BookOrderTable {
  class BooksByOrder(tag: Tag) extends Table[BookByOrder](tag, "books_by_order") {
    def id: Rep[Option[Long]] = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def orderId: Rep[Long] = column[Long]("order_id")
    def bookId: Rep[Long] = column[Long]("book_id")
    def unitPrice: Rep[Double] = column[Double]("unit_price_usd")
    def quantity: Rep[Int] = column[Int]("quantity")

    override def * : ProvenShape[BookByOrder] =
      (id, orderId, bookId, unitPrice, quantity) <> ((BookByOrder.apply _).tupled, BookByOrder.unapply)
  }

  protected val booksByOrder = TableQuery[BooksByOrder]
}
