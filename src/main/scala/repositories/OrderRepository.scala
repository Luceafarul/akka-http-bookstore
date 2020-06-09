package repositories

import models.{Book, BookByOrder, BookOrderTable, Order, OrderTable, OrderWithBooks}
import services.DatabaseService

import scala.concurrent.{ExecutionContext, Future}

class OrderRepository(databaseService: DatabaseService)(implicit val ec: ExecutionContext)
  extends OrderTable
    with BookOrderTable {

  import databaseService._
  import slick.jdbc.PostgresProfile.api._

  def findOrdersByUser(userId: Long): Future[Seq[Order]] =
    db.run(orders.filter(_.userId === userId).result)

  def findBooksByOrder(orderId: Long): Future[Seq[BookByOrder]] =
    db.run(booksByOrder.filter(_.orderId === orderId).result)

  def createOrder(orderWithBooks: OrderWithBooks): Future[Order] = {
    val dbAction = {
      for {
        o <- orders returning orders += orderWithBooks.order
        _ <- booksByOrder returning booksByOrder ++= orderWithBooks.books.groupBy(_.id) map {
          case (Some(_), books) => prepareBooks(o.id.get, books.head, books.size)
        }
      } yield o
    }.transactionally

    db.run(dbAction)
  }

  private def prepareBooks(orderId: Long, book: Book, quantity: Int): BookByOrder = {
    BookByOrder(
      id = None,
      orderId = orderId,
      bookId = book.id.get,
      unitPrice = book.price,
      quantity = quantity
    )
  }
}
