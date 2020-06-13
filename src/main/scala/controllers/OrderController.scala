package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers
import models.{OrderJson, OrderWithBooks}
import repositories.OrderRepository
import services.TokenService
import web.directives.VerifyToken

import scala.concurrent.ExecutionContext

class OrderController(val orderRepository: OrderRepository, val tokenService: TokenService)
                     (implicit val executor: ExecutionContext) extends OrderJson
  with PredefinedFromStringUnmarshallers
  with VerifyToken {

  val routes: Route = pathPrefix("orders") {
    verifyToken { user =>
      pathEndOrSingleSlash {
        post {
          (decodeRequest & entity(as[OrderWithBooks])) { createOrder =>
            complete(StatusCodes.Created, orderRepository.createOrder(createOrder))
          }
        } ~
        get {
          complete(orderRepository.findOrdersByUser(user.id.get))
        }
      } ~ path(LongNumber / "books") { orderId =>
        get {
          complete(orderRepository.findBooksByOrder(orderId))
        }
      }
    }
  }
}
