package utils

import cats.implicits._
import eu.timepit.refined.api.Refined
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops.toCoercibleIdOps
import model.brand.{Brand, BrandId, BrandName}
import model.card.{
  Card,
  CardVerifValue,
  CardVerifValuePred,
  CardExpiration,
  CardExpirationPred,
  CardName,
  CardNamePred,
  CardNumber,
  CardNumberPred
}
import model.cart.{CartItem, CartTotal, Quantity}
import model.category.{Category, CategoryId, CategoryName}
import model.item.{Item, ItemDescription, ItemId, ItemName}
import org.scalacheck.Gen
import squants.market.{Money, USD}

import java.util.UUID

object Generators {

  def cbUuid[A: Coercible[UUID, *]]: Gen[A] =
    Gen.uuid.map(_.coerce[A])

  def cbStr[A: Coercible[String, *]]: Gen[A] =
    genNonEmptyString.map(_.coerce[A])

  def cbInt[A: Coercible[Int, *]]: Gen[A] =
    Gen.posNum[Int].map(_.coerce[A])

  val genNonEmptyString: Gen[String] = {
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.stringOfN(n, Gen.alphaChar)
      }
  }

  val brandGen: Gen[Brand] =
    for {
      id <- cbUuid[BrandId]
      name <- cbStr[BrandName]
    } yield Brand(id, name)

  val categoryGen: Gen[Category] =
    for {
      id <- cbUuid[CategoryId]
      name <- cbStr[CategoryName]
    } yield Category(id, name)

  val moneyGen: Gen[Money] =
    Gen.posNum[Long].map(n => USD(n))

  val itemGen: Gen[Item] = {
    for {
      id <- cbUuid[ItemId]
      name <- cbStr[ItemName]
      description <- cbStr[ItemDescription]
      price <- moneyGen
      brand <- brandGen
      category <- categoryGen
    } yield Item(id, name, description, price, brand, category)
  }

  val cartItemGen: Gen[CartItem] = {
    for {
      item <- itemGen
      qty <- cbInt[Quantity]
    } yield CartItem(item, qty)
  }

  val cartTotalGen: Gen[CartTotal] = {
    for {
      items <- Gen.nonEmptyListOf(cartItemGen)
      total <- Gen.const(
        USD(items.foldMap(i => i.item.price.value * i.quantity.value))
      )
    } yield CartTotal(items, total)
  }

  val cardGen: Gen[Card] =
    for {
      name <- genNonEmptyString.map[CardNamePred](Refined.unsafeApply)
      number <- Gen.posNum[Long].map[CardNumberPred](Refined.unsafeApply)
      exp <- Gen.posNum[Int].map[CardExpirationPred](Refined.unsafeApply)
      cvv <- Gen.posNum[Int].map[CardVerifValuePred](Refined.unsafeApply)
    } yield Card(
      CardName(name),
      CardNumber(number),
      CardExpiration(exp),
      CardVerifValue(cvv)
    )
}
