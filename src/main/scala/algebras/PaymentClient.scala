package algebras

import effects._
import model.order.{PaymentError, PaymentId}
import model.Payment
import org.http4s.Method.GET
import org.http4s.{Status, Uri}
import org.http4s.circe.{JsonDecoder, toMessageSynax}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import cats.implicits._
import config.model.PaymentConfig
import http._

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

class LivePaymentClient[F[_]: JsonDecoder: MonadThrow: BracketThrow](
    client: Client[F],
    paymentConfig: PaymentConfig
) extends PaymentClient[F]
    with Http4sClientDsl[F] {
  def process(payment: Payment): F[PaymentId] = {
    Uri
      .fromString(paymentConfig.uri.value.value)
      .liftTo[F]
      .flatMap { uri =>
        GET(uri).flatMap { req =>
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
}
