package utils

import model.brand.Brand
import model.card.Card
import model.cart.CartTotal
import model.order.{OrderId, PaymentId}
import model.user.UserId
import org.scalacheck.Arbitrary
import utils.Generators.{brandGen, cardGen, cartTotalGen, cbUuid}

object Arbitraries {

  implicit val arbCartTotal: Arbitrary[CartTotal] =
    Arbitrary(cartTotalGen)

  implicit val arbCard: Arbitrary[Card] =
    Arbitrary(cardGen)

  implicit val arbBrand: Arbitrary[Brand] =
    Arbitrary(brandGen)

  implicit val arbPaymentId: Arbitrary[PaymentId] = Arbitrary(cbUuid[PaymentId])
  implicit val arbUserId: Arbitrary[UserId] = Arbitrary(cbUuid[UserId])
  implicit val arbOrderId: Arbitrary[OrderId] = Arbitrary(cbUuid[OrderId])
}
