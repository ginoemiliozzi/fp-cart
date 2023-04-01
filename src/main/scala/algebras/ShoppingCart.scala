package algebras

import cats.ApplicativeError
import cats.effect.Sync
import dev.profunktor.redis4cats.RedisCommands
import effects.MonadThrow
import model.cart.{Cart, CartItem, CartTotal, Quantity}
import model.item.ItemId
import model.user.UserId
import cats.syntax.all._
import config.model.ShoppingCartExpiration
import squants.market.{Money, USD}
import utils.Utils.readCoercUUID

trait ShoppingCart[F[_]] {
  def add(
      userId: UserId,
      itemId: ItemId,
      quantity: Quantity
  ): F[Unit]
  def delete(userId: UserId): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

final class LiveShoppingCart[F[_]: Sync: MonadThrow] private (
    items: Items[F],
    redis: RedisCommands[F, String, String],
    exp: ShoppingCartExpiration
) extends ShoppingCart[F] {

  private def calcTotal(items: List[CartItem]): Money =
    USD(
      items
        .foldMap { i =>
          i.item.price.value * i.quantity.value
        }
    )

  override def add(
      userId: UserId,
      itemId: ItemId,
      quantity: Quantity
  ): F[Unit] = {
    redis.hSet(
      userId.value.toString,
      itemId.value.toString,
      quantity.value.toString
    ) *>
      redis.expire(
        userId.value.toString,
        exp.value
      )
  }

  override def delete(userId: UserId): F[Unit] =
    redis.del(userId.value.toString)

  override def get(userId: UserId): F[CartTotal] = {
    redis
      .hGetAll(userId.value.toString)
      .flatMap(itMap =>
        itMap.toList
          .traverseFilter { case (iId, qty) =>
            for {
              itemId <- readCoercUUID[F, ItemId](iId)
              quantity <- ApplicativeError[F, Throwable]
                .catchNonFatal(Quantity(qty.toInt))
              cartItemO <- items
                .findById(itemId)
                .map(_.map(i => CartItem(i, quantity)))
            } yield cartItemO
          }
          .map(cartItems =>
            CartTotal(items = cartItems, total = calcTotal(cartItems))
          )
      )

  }

  override def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    redis.hDel(userId.value.toString, itemId.value.toString)

  override def update(userId: UserId, cart: Cart): F[Unit] = {
    redis.hGetAll(userId.value.toString).flatMap { its =>
      its.toList.traverse_ { case (k, _) =>
        readCoercUUID[F, ItemId](k).flatMap { itemId =>
          cart.items.get(itemId).traverse_ { qty =>
            redis.hSet(userId.value.toString, k, qty.value.toString)
          }
        }
      } *>
        redis.expire(userId.value.toString, exp.value)
    }
  }
}

object LiveShoppingCart {
  def make[F[_]: Sync](
      items: Items[F],
      redisCommands: RedisCommands[F, String, String],
      expiration: ShoppingCartExpiration
  ): F[ShoppingCart[F]] =
    Sync[F].delay(new LiveShoppingCart(items, redisCommands, expiration))
}
