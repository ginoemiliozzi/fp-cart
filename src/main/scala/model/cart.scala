package model

import io.estatico.newtype.macros.newtype
import model.item.{Item, ItemId}
import model.user.UserId
import squants.market.Money

import java.util.UUID
import scala.util.control.NoStackTrace

object cart {
  @newtype case class Quantity(value: Int)
  @newtype case class Cart(items: Map[ItemId, Quantity])
  @newtype case class CartId(value: UUID)
  case class CartItem(item: Item, quantity: Quantity)
  case class CartTotal(items: List[CartItem], total: Money)

  case class CartNotFound(userId: UserId) extends NoStackTrace
}
