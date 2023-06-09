package http.routes

import algebras.Categories
import cats.{Defer, Monad}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import http._

final class CategoryRoutes[F[_]: Monad: Defer](categories: Categories[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/categories"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok(categories.findAll)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
