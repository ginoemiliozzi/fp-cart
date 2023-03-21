package model

import io.estatico.newtype.macros.newtype
import model.cart.Quantity
import model.item.ItemId
import squants.market.Money

import java.util.UUID
import scala.util.control.NoStackTrace

object order {

  @newtype case class OrderId(uuid: UUID)
  @newtype case class PaymentId(uuid: UUID)
  case class Order(
      id: OrderId,
      pid: PaymentId,
      items: Map[ItemId, Quantity],
      total: Money
  )

  case object EmptyCartError extends NoStackTrace

  case class OrderError(cause: String) extends NoStackTrace

  case class PaymentError(cause: String) extends NoStackTrace
}
