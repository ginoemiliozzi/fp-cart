package programs

import algebras.{Orders, PaymentClient, ShoppingCart}
import cats.effect.Timer
import model.order.{EmptyCartError, OrderError, OrderId, PaymentError, PaymentId}
import model.user.UserId
import cats.implicits._
import effects.{Background, MonadThrow}
import io.chrisdavenport.log4cats.Logger
import model.card.Card
import model.Payment
import model.cart.{CartItem, CartTotal}
import retry.{RetryDetails, RetryPolicy, retryingOnAllErrors}
import retry.RetryDetails.{GivingUp, WillDelayAndRetry}
import retry.RetryPolicies._
import squants.market.Money

import scala.concurrent.duration.DurationInt

final class CheckoutProgram[F[_]: MonadThrow: Logger: Background: Timer](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F]
) {
  val retryPolicy: RetryPolicy[F] =
    limitRetries[F](3) |+| exponentialBackoff[F](10.milliseconds)

  private def logError(
      action: String
  )(e: Throwable, details: RetryDetails): F[Unit] =
    details match {
      case r: WillDelayAndRetry =>
        Logger[F].error(
          s"Failed to process $action with ${e.getMessage}. So far we have retried ${r.retriesSoFar} times."
        )
      case g: GivingUp =>
        Logger[F].error(
          s"Giving up on $action after ${g.totalRetries} retries."
        )
    }

  private def withRetryPolicy[A](action: String)(fa: => F[A]): F[A] =
    retryingOnAllErrors[A](
      policy = retryPolicy,
      onError = logError(action)
    )(fa)

  private def processPayment(payment: Payment): F[PaymentId] = {
    val action = withRetryPolicy[PaymentId]("Payment")(
      paymentClient.process(payment)
    )

    action.adaptError { case e =>
      PaymentError(Option(e.getMessage).getOrElse("Unknown"))
    }
  }

  private def createOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: Money
  ): F[OrderId] = {
    val action = withRetryPolicy[OrderId]("Order")(
      orders.create(userId, paymentId, items, total)
    )

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.adaptError { case e =>
        OrderError(e.getMessage)
      }.onError { case _ =>
        Logger[F].error(
          s"Failed to create order for Payment: ${paymentId}. Rescheduling as a background action"
        ) *>
          Background[F].schedule(bgAction(fa), 1.hour)
      }

    bgAction(action)
  }

  def checkout(userId: UserId, card: Card): F[OrderId] =
    shoppingCart
      .get(userId)
      .ensure(EmptyCartError)(_.items.nonEmpty)
      .flatMap { case CartTotal(items, total) =>
        for {
          paymentId <- processPayment(Payment(userId, total, card))
          orderId <- createOrder(userId, paymentId, items, total)
          _ <- shoppingCart.delete(userId)
        } yield orderId
      }
}
