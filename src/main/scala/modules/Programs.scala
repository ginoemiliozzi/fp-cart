package modules

import cats.effect._
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import retry.RetryPolicy
import retry.RetryPolicies._
import effects._
import eu.timepit.refined.types.all.PosInt
import programs._

import scala.concurrent.duration.FiniteDuration

case class CheckoutConfig(
    retriesLimit: PosInt,
    retriesBackoff: FiniteDuration
)

object Programs {
  def make[F[_]: Background: Logger: Sync: Timer](
      checkoutConfig: CheckoutConfig,
      algebras: Algebras[F],
      clients: HttpClients[F]
  ): F[Programs[F]] =
    Sync[F].delay(
      new Programs[F](checkoutConfig, algebras, clients)
    )
}

final class Programs[F[_]: Background: Logger: MonadThrow: Timer] private (
    cfg: CheckoutConfig,
    algebras: Algebras[F],
    clients: HttpClients[F]
) {

  val retryPolicy: RetryPolicy[F] =
    limitRetries[F](cfg.retriesLimit.value) |+| exponentialBackoff[F](
      cfg.retriesBackoff
    )

  val checkout: CheckoutProgram[F] = new CheckoutProgram[F](
    clients.payment,
    algebras.cart,
    algebras.orders,
    retryPolicy
  )

}
