package http.routes

import algebras.ShoppingCart
import cats.{Defer, Monad}
import cats.syntax.all._
import model.cart.Cart
import model.item.ItemId
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.{JsonDecoder, toMessageSynax}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import http.users.CommonUser
import http._

final class CartRoutes[F[_]: Defer: JsonDecoder: Monad](
    shoppingCart: ShoppingCart[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {

      // Get shopping cart
      case GET -> Root as user =>
        Ok(shoppingCart.get(user.value.id))

      // Add items to the cart
      case ar @ POST -> Root as user =>
        ar.req.asJsonDecode[Cart].flatMap { cart =>
          cart.items
            .map { case (id, quantity) =>
              shoppingCart
                .add(user.value.id, id, quantity)
            }
            .toList
            .sequence *> Created()
        }

      // Modify items in the cart
      case ar @ PUT -> Root as user =>
        ar.req.asJsonDecode[Cart].flatMap { cart =>
          shoppingCart
            .update(user.value.id, cart) *> Ok()
        }

      // Remove item from the cart
      case DELETE -> Root / UUIDVar(uuid) as user =>
        shoppingCart.removeItem(
          user.value.id,
          ItemId(uuid)
        ) *> NoContent()
    }

  def routes(
      authMiddleware: AuthMiddleware[F, CommonUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
