package http.routes

import algebras.auth.Auth
import cats.Defer
import effects.MonadThrow
import http.users.{CreateUser, UsernameInUse}
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import http._
import cats.implicits._

final class UserRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    auth: Auth[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"
  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root / "users" =>
      req
        .decodeR[CreateUser] { user =>
          auth
            .newUser(
              user.username.toDomain,
              user.password.toDomain
            )
            .flatMap(Created(_))
            .recoverWith { case UsernameInUse(u) =>
              Conflict(u.value)
            }
        }
    }
  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
