package utils.mocks

import algebras.ShoppingCart
import cats.effect.IO
import model.cart.{Cart, CartTotal, Quantity}
import model.item.ItemId
import model.user.UserId
import squants.market.USD

object shoppingCart {

  def successfulCart(cartTotal: CartTotal): ShoppingCart[IO] =
    new ShoppingCart[IO] {
      override def add(
          userId: UserId,
          itemId: ItemId,
          quantity: Quantity
      ): IO[Unit] = IO.unit

      override def delete(userId: UserId): IO[Unit] = IO.unit

      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)

      override def removeItem(userId: UserId, itemId: ItemId): IO[Unit] =
        IO.unit

      override def update(userId: UserId, cart: Cart): IO[Unit] = IO.unit
    }

  val emptyCart: ShoppingCart[IO] = {
    new ShoppingCart[IO] {
      override def add(
          userId: UserId,
          itemId: ItemId,
          quantity: Quantity
      ): IO[Unit] = IO.unit

      override def delete(userId: UserId): IO[Unit] = IO.unit

      override def get(userId: UserId): IO[CartTotal] =
        IO.pure(CartTotal(List.empty, USD(0)))

      override def removeItem(userId: UserId, itemId: ItemId): IO[Unit] =
        IO.unit

      override def update(userId: UserId, cart: Cart): IO[Unit] = IO.unit
    }
  }
}
