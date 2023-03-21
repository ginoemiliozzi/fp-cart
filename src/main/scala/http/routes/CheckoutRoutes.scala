package http.routes

import cats.Defer
import effects.MonadThrow
import http.users.CommonUser
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import programs.CheckoutProgram
import model.cart.CartNotFound
import model.order.{EmptyCartError, OrderError, PaymentError}
import org.http4s.server.{AuthMiddleware, Router}
import http._
import cats.syntax.all._
import model.card.Card

final class CheckoutRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    checkoutProgram: CheckoutProgram[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/checkout"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of { case ar @ POST -> Root as user =>
      ar.req.decodeR[Card] { card =>
        checkoutProgram
          .checkout(user.value.id, card)
          .flatMap(Created(_))
          .recoverWith {
            case CartNotFound(userId) =>
              NotFound(
                s"Cart not found for user: ${userId.value}"
              )
            case EmptyCartError =>
              BadRequest("Shopping cart is empty!")
            case PaymentError(cause) =>
              BadRequest(cause)
            case OrderError(cause) =>
              BadRequest(cause)
          }
      }
    }
  def routes(
      authMiddleware: AuthMiddleware[F, CommonUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
