package modules

import algebras.{LivePaymentClient, PaymentClient}
import cats.effect._
import org.http4s.client.Client

object HttpClients {
  def make[F[_]: Sync](
      client: Client[F]
  ): F[HttpClients[F]] =
    Sync[F].delay(
      new HttpClients[F] {
        def payment: PaymentClient[F] = new LivePaymentClient[F](client)
      }
    )
}

trait HttpClients[F[_]] {
  def payment: PaymentClient[F]
}
