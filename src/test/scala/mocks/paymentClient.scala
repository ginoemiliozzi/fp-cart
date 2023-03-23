package mocks

import algebras.PaymentClient
import cats.effect.IO
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
}
