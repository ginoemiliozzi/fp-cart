package http.routes.auth

import algebras.auth.AuthCreds
import cats.Defer
import cats.syntax.all._
import effects.MonadThrow
import http._
import http.users.{InvalidUserOrPassword, LoginUser}
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class LoginRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    auth: AuthCreds[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
      req.decodeR[LoginUser] { user =>
        auth
          .login(user.username.toDomain, user.password.toDomain)
          .flatMap(Ok(_))
          .recoverWith { case InvalidUserOrPassword(_) =>
            Forbidden("Invalid user or password")
          }
      }
    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
