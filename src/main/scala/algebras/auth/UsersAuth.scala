package algebras.auth

import cats.{Applicative, Functor}
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import http._
import http.users.{AdminUser, CommonUser}
import model.user.{User, UserId, UserName}
import pdi.jwt.JwtClaim
import io.circe.parser.decode

import java.util.UUID

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

class LiveUsersAuth[F[_]: Functor](
    redis: RedisCommands[F, String, String]
) extends UsersAuth[F, CommonUser] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
    redis
      .get(token.value)
      .map(_.flatMap { u =>
        decode[User](u).toOption.map(CommonUser.apply)
      })
}

class LiveAdminAuth[F[_]: Applicative]() extends UsersAuth[F, AdminUser] {

  // TODO move to config - there is only one admin created manually
  val adminToken = JwtToken(
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1dWlkIjoiYTY3M2NiM2MtNWExNy00NGJiLTgzMDYtYWQ4ODQ2YjZhMjBkIn0.4mUB7wvus-s3bHMjmb382R0qoIzg82Taq-6OeMfYn_w"
  )
  val adminUser = AdminUser(
    User(
      UserId(UUID.fromString("a673cb3c-5a17-44bb-8306-ad8846b6a20d")),
      UserName("admin")
    )
  )
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
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
  def make[F[_]: Sync](): F[UsersAuth[F, AdminUser]] =
    Sync[F].delay(new LiveAdminAuth())
}
