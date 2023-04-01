package utils.mocks

import algebras.Orders
import cats.effect.IO
import cats.implicits.none
import model.cart.CartItem
import model.order
import model.order.{Order, OrderError, OrderId, PaymentId}
import model.user.UserId
import squants.market.Money

object orders {
  def successfulOrders(oid: OrderId): Orders[IO] = new Orders[IO] {
    override def get(
        userId: UserId,
        orderId: OrderId
    ): IO[Option[Order]] = IO.pure(none)

    override def findBy(userId: UserId): IO[List[Order]] = IO.pure(List.empty)

    override def create(
        userId: UserId,
        paymentId: PaymentId,
        items: List[CartItem],
        total: Money
    ): IO[order.OrderId] = IO.pure(oid)
  }

  val failingOrders: Orders[IO] =
    new Orders[IO] {
      override def get(userId: UserId, orderId: OrderId): IO[Option[Order]] =
        IO.none
      override def findBy(userId: UserId): IO[List[Order]] = IO.pure(List.empty)

      override def create(
          userId: UserId,
          paymentId: PaymentId,
          items: List[CartItem],
          total: Money
      ): IO[OrderId] = IO.raiseError(OrderError("boom!"))
    }
}
