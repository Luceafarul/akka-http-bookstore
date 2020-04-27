package web

import akka.actor.ActorSystem
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

trait WebApi {
  implicit def system: ActorSystem
  implicit def materializer: Materializer
  implicit def executor: ExecutionContext
}
