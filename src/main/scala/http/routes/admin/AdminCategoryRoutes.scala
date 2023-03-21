package http.routes.admin

import algebras.Categories
import cats.Defer
import effects.MonadThrow
import http.RefinedRequestDecoder
import http.users.AdminUser
import model.category.CategoryParam
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import http._

final class AdminCategoryRoutes[
    F[_]: Defer: JsonDecoder: MonadThrow
](
    categories: Categories[F]
) extends Http4sDsl[F] {
  private[admin] val prefixPath = "/categories"
  private val httpRoutes: AuthedRoutes[AdminUser, F] =
    AuthedRoutes.of { case ar @ POST -> Root as _ =>
      ar.req.decodeR[CategoryParam] { c =>
        Created(categories.create(c.toDomain))
      }
    }

  def routes(
      authMiddleware: AuthMiddleware[F, AdminUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
