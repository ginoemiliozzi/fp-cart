package model

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.MatchesRegex
import io.estatico.newtype.macros.newtype

object card {

  type Rgx = W.`"^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"`.T
  type CardNamePred = String Refined MatchesRegex[Rgx]
  type CardNumberPred = Long Refined Size[16]
  type CardExpirationPred = Int Refined Size[4]
  type CardVerifValuePred = Int Refined Size[3]

  @newtype case class CardName(value: CardNamePred)

  @newtype case class CardNumber(value: CardNumberPred)

  @newtype case class CardExpiration(value: CardExpirationPred)

  @newtype case class CardVerifValue(value: CardVerifValuePred)

  case class Card(
      name: CardName,
      number: CardNumber,
      expiration: CardExpiration,
      cvv: CardVerifValue
  )
}
