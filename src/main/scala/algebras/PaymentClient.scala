package algebras

import cats.effect.Sync
import effects._
import model.order.{PaymentError, PaymentId}
import model.Payment
import org.http4s.Method.POST
import org.http4s.{Status, Uri}
import org.http4s.circe.{JsonDecoder, toMessageSynax}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import cats.implicits._
import http._
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

class LivePaymentClient[F[_]: JsonDecoder: MonadThrow: BracketThrow](
    client: Client[F]
) extends PaymentClient[F]
    with Http4sClientDsl[F] {
  private val baseUri = "http://localhost:8080/api/v1"
  def process(payment: Payment): F[PaymentId] =
    Uri
      .fromString(baseUri + "/payments")
      .liftTo[F]
      .flatMap { uri =>
        POST(payment, uri).flatMap { req =>
          client
            .run(req)
            .use { res =>
              if (res.status == Status.Ok || res.status == Status.Conflict)
                res.asJsonDecode[PaymentId]
              else
                PaymentError(
                  Option(res.status.reason).getOrElse("Unknown")
                ).raiseError[F, PaymentId]
            }
        }
      }
}

object LivePaymentClient {
  def make[F[_]: Sync](): F[LivePaymentClient[F]] = {
    BlazeClientBuilder[F](ExecutionContext.global).resource
      .use { client =>
        Sync[F].delay {
          new LivePaymentClient[F](client)
        }
      }
  }
}
