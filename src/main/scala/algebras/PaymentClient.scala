package algebras

import cats.effect.Sync
import effects._
import model.order.{PaymentError, PaymentId}
import model.Payment
import org.http4s.Method.GET
import org.http4s.Uri
import org.http4s.circe.JsonDecoder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import cats.implicits._
import config.model.PaymentConfig

import io.circe.parser.decode
import java.util.UUID

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

class LivePaymentClient[F[_]: JsonDecoder: Sync: MonadThrow: BracketThrow](
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
            .expect[String](req)
            .flatMap { res =>
              decode[List[UUID]](res) match {
                case Right(uuidList) if uuidList.nonEmpty =>
                  PaymentId(uuidList.head).pure[F]
              }
            }
            .recoverWith { case e =>
              PaymentError(
                Option(e.getMessage).getOrElse("Unknown payment error")
              ).raiseError[F, PaymentId]
            }
        }
      }
  }
}
