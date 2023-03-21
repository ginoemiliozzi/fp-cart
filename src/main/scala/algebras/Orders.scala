package algebras

import cats.effect.{Resource, Sync}
import cats.implicits._
import model._
import http._
import model.cart.{CartItem, Quantity}
import model.item.ItemId
import model.order.{Order, OrderId, PaymentId}
import model.user.UserId
import skunk._
import skunk.circe.codec.all.jsonb
import skunk.codec.all._
import skunk.implicits._
import squants.market.{Money, USD}
import utils.Utils.genCoercUUID

import java.util.UUID

trait Orders[F[_]] {
  def get(
      userId: UserId,
      orderId: OrderId
  ): F[Option[Order]]
  def findBy(userId: UserId): F[List[Order]]
  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: Money
  ): F[OrderId]
}

private class LiveOrders[F[_]: Sync](
    sessionPool: Resource[F, Session[F]]
) extends Orders[F] {
  import OrderQueries._

  def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
    sessionPool.use { session =>
      session.prepare(selectByUserIdAndOrderId).use { q =>
        q.option(userId ~ orderId)
      }
    }
  def findBy(userId: UserId): F[List[Order]] =
    sessionPool.use { session =>
      session.prepare(selectByUserId).use { q =>
        q.stream(userId, 1024).compile.toList
      }
    }
  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: Money
  ): F[OrderId] =
    sessionPool.use { session =>
      session.prepare(insertOrder).use { cmd =>
        genCoercUUID[F, OrderId].flatMap { id =>
          val itMap: Map[ItemId, Quantity] =
            items.map(x => x.item.uuid -> x.quantity).toMap
          val order = Order(id, paymentId, itMap, total)
          cmd.execute(userId ~ order).as(id)
        }
      }
    }
}

object LiveOrders {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Orders[F]] =
    Sync[F].delay(
      new LiveOrders[F](sessionPool)
    )
}

object OrderQueries {
  val orderDecoder: Decoder[Order] =
    (
      uuid.cimap[OrderId] ~ uuid ~ uuid.cimap[PaymentId] ~
        jsonb[Map[ItemId, Quantity]] ~ numeric.map(USD.apply)
    ).map { case o ~ _ ~ p ~ i ~ t =>
      Order(o, p, i, t)
    }

  val selectByUserId: Query[UserId, Order] =
    sql"""
      SELECT * FROM orders
      WHERE user_id = ${uuid.cimap[UserId]}
    """.query(orderDecoder)

  val selectByUserIdAndOrderId: Query[UserId ~ OrderId, Order] =
    sql"""
      SELECT * FROM orders
      WHERE user_id = ${uuid.cimap[UserId]}
      AND uuid = ${uuid.cimap[OrderId]}
    """.query(orderDecoder)

  val encoder: Encoder[UserId ~ Order] =
    (
      uuid.cimap[OrderId] ~ uuid.cimap[UserId] ~
        uuid.cimap[PaymentId] ~ jsonb[Map[ItemId, Quantity]] ~
        numeric.contramap[Money](_.amount)
    ).contramap { case id ~ o =>
      o.id ~ id ~ o.pid ~ o.items ~ o.total
    }

  val insertOrder: Command[UserId ~ Order] =
    sql"""
      INSERT INTO orders
      VALUES ($encoder)
    """.command
}
