package algebras.auth

import cats.{Applicative, Functor}
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import http._
import http.users.{AdminUser, CommonUser}
import model.user.User
import pdi.jwt.JwtClaim
import io.circe.parser.decode

trait UsersAuth[F[_], A] {
  def userSessionData(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

class LiveUsersAuth[F[_]: Functor](
    redis: RedisCommands[F, String, String]
) extends UsersAuth[F, CommonUser] {
  def userSessionData(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
    redis
      .get(token.value)
      .map(_.flatMap { u =>
        decode[User](u).toOption.map(CommonUser.apply)
      })
}

class LiveAdminAuth[F[_]: Applicative](
    adminToken: JwtToken,
    adminUser: AdminUser
) extends UsersAuth[F, AdminUser] {

  def userSessionData(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
    Applicative[F].pure {
      (token == adminToken)
        .guard[Option]
        .as(adminUser)
    }
}

object LiveUsersAuth {
  def make[F[_]: Sync](
      redis: RedisCommands[F, String, String]
  ): F[UsersAuth[F, CommonUser]] =
    Sync[F].delay(new LiveUsersAuth(redis))
}

object LiveAdminAuth {
  def make[F[_]: Sync](
      adminToken: JwtToken,
      adminUser: AdminUser
  ): F[UsersAuth[F, AdminUser]] =
    Sync[F].delay(new LiveAdminAuth(adminToken, adminUser))
}
