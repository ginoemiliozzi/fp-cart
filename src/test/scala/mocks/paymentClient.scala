package mocks

import algebras.PaymentClient
import cats.effect.IO
import cats.effect.concurrent.Ref
import model.Payment
import model.order.{PaymentError, PaymentId}

object paymentClient {
  def successfulClient(paymentId: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        IO.pure(paymentId)
    }

  val unreachableClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        IO.raiseError(PaymentError("boom!"))
    }

  def failNTimesBeforeSuccess(
      failN: Int,
      attemptsCountRef: Ref[IO, Int],
      paymentId: PaymentId
  ): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        attemptsCountRef.get.flatMap { attempts =>
          if (attempts < failN)
            attemptsCountRef
              .update(_ + 1) *> IO.raiseError[PaymentId](PaymentError("boom!"))
          else
            attemptsCountRef
              .update(_ + 1)
              .map(_ => paymentId)
        }
    }
}
