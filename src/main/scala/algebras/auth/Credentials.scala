package algebras.auth

import algebras.{Tokens, Users}
import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import config.model.TokenExpiration
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import effects.MonadThrow
import http._
import http.users.{InvalidUserOrPassword, UsernameInUse}
import io.circe.syntax.EncoderOps
import model.user.{Password, User, UserName}

trait AuthCreds[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]
}

final class LiveAuthCreds[F[_]: MonadThrow] private (
    tokenExpiration: TokenExpiration,
    tokens: Tokens[F],
    users: Users[F],
    redis: RedisCommands[F, String, String]
) extends AuthCreds[F] {
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
            tk <- tokens.create(userId)
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
            case Some(tk) => Applicative[F].pure(JwtToken(tk))
            case None =>
              tokens
                .create(user.id)
                .flatTap(token => setLoggedInUserRedis(user, token))
          }
      }
  }

  override def logout(token: JwtToken, username: UserName): F[Unit] =
    redis.del(username.value) *> redis.del(token.value)
}

object LiveAuthCreds {
  def make[F[_]: Sync](
      tokenExpiration: TokenExpiration,
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String]
  ): F[AuthCreds[F]] =
    Sync[F].delay(
      new LiveAuthCreds(tokenExpiration, tokens, users, redis)
    )
}
