package algebras.auth

import algebras.{Tokens, Users}
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import effects.MonadThrow
import http._
import http.users.{InvalidUserOrPassword, UsernameInUse}
import io.circe.syntax.EncoderOps
import model.user.{Password, TokenExpiration, User, UserName}

trait Auth[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]
}

final class LiveAuth[F[_]: MonadThrow] private (
    tokenExpiration: TokenExpiration,
    tokens: Tokens[F],
    users: Users[F],
    redis: RedisCommands[F, String, String]
) extends Auth[F] {
  private val TokenExpiration = tokenExpiration.value

  private def setLoggedInUserRedis(user: User, token: JwtToken): F[Unit] = {
    redis.setEx(
      token.value,
      user.asJson.noSpaces,
      TokenExpiration
    ) *> redis.setEx(user.name.value, token.value, TokenExpiration)
  }

  override def newUser(username: UserName, password: Password): F[JwtToken] = {
    users
      .find(username)
      .flatMap {
        case Some(_) => UsernameInUse(username).raiseError[F, JwtToken]
        case None =>
          for {
            userId <- users.create(username, password)
            tk <- tokens.create
            u = User(userId, username)
            _ <- setLoggedInUserRedis(u, tk)
          } yield tk
      }
  }

  override def login(username: UserName, password: Password): F[JwtToken] = {
    users
      .authorize(username, password)
      .flatMap {
        case None => InvalidUserOrPassword(username).raiseError[F, JwtToken]
        case Some(user) =>
          redis.get(username.value).flatMap {
            case Some(tk) => JwtToken(tk).pure
            case None =>
              tokens.create.flatTap(token => setLoggedInUserRedis(user, token))
          }
      }
  }

  override def logout(token: JwtToken, username: UserName): F[Unit] =
    redis.del(username.value) *> redis.del(token.value)
}

object LiveAuth {
  def make[F[_]: Sync](
      tokenExpiration: TokenExpiration,
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String]
  ): F[Auth[F]] =
    Sync[F].delay(
      new LiveAuth(tokenExpiration, tokens, users, redis)
    )
}
