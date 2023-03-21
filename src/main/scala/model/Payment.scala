package model

import model.card.Card
import model.user.UserId
import squants.market.Money

case class Payment(
    id: UserId,
    total: Money,
    card: Card
)
