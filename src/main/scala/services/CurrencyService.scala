package services

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}

object CurrencyService {

  private case class RatesAPIResponse(rates: Map[String, Float], base: String, date: LocalDate)

  private object RatesAPIResponse extends SprayJsonSupport with DefaultJsonProtocol {
    import converters.JsonConverters.LocalDateJsonFormat

    implicit val ratesAPIResponseJsonFormat = jsonFormat3(RatesAPIResponse.apply)
  }

  val baseCurrency = "USD"

  val supportedCurrencies = List("USD", "EUR")

  val exchangeCurrencies = supportedCurrencies.filterNot(_ == baseCurrency)

  private val url = s"https://api.exchangeratesapi.io/latest?base=$baseCurrency&symbols=${exchangeCurrencies.mkString(",")}"

  private val request = HttpRequest(HttpMethods.GET, url)

  def rates(implicit ex: ExecutionContext, as: ActorSystem, mat: Materializer): Future[Option[Map[String, Float]]] = {
    Http().singleRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK if response.entity.contentType == ContentTypes.`application/json` =>
          Unmarshal(response.entity).to[RatesAPIResponse].map { resp =>
            if (resp.rates.isEmpty) None
            else Some(resp.rates)
          }
        case _ => Future.successful(None)
      }
    }
  }
}
