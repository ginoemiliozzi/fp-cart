package http.routes.admin

import algebras.Items
import cats.Defer
import effects.MonadThrow
import http.RefinedRequestDecoder
import http.users.AdminUser
import model.item.{CreateItemParam, UpdateItemParam}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import http._

final class AdminItemRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    items: Items[F]
) extends Http4sDsl[F] {
  private[admin] val prefixPath = "/items"
  private val httpRoutes: AuthedRoutes[AdminUser, F] =
    AuthedRoutes.of {
      // Create new item
      case ar @ POST -> Root as _ =>
        ar.req.decodeR[CreateItemParam] { item =>
          Created(items.create(item.toDomain))
        }

      // Update price of item
      case ar @ PUT -> Root as _ =>
        ar.req.decodeR[UpdateItemParam] { item =>
          Ok(items.update(item.toDomain))
        }
    }
  def routes(
      authMiddleware: AuthMiddleware[F, AdminUser]
  ): HttpRoutes[F] = Router(prefixPath -> authMiddleware(httpRoutes))
}
