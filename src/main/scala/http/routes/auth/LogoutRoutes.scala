package http.routes.auth

import algebras.auth.AuthCreds
import cats.syntax.all._
import cats.{Defer, Monad}
import dev.profunktor.auth.AuthHeaders
import http.users.CommonUser
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final class LogoutRoutes[F[_]: Defer: Monad](
    auth: AuthCreds[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of { case ar @ POST -> Root / "logout" as user =>
      AuthHeaders
        .getBearerToken(ar.req)
        .traverse_(t => auth.logout(t, user.value.name)) *> NoContent()
    }

  def routes(
      authMiddleware: AuthMiddleware[F, CommonUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
