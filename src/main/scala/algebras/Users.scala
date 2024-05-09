package algebras

import cats.effect.{Resource, Sync}
import cats.implicits._
import effects.BracketThrow
import http.users.UsernameInUse
import model._
import model.user.{EncryptedPassword, Password, User, UserId, UserName}
import skunk._
import skunk.codec.all._
import skunk.implicits._
import utils.Utils.genCoercUUID

trait Users[F[_]] {
  def find(
      username: UserName
  ): F[Option[User]]
  def authorize(
      username: UserName,
      password: Password
  ): F[Option[User]]
  def create(
      username: UserName,
      password: Password
  ): F[UserId]
}

final class LiveUsers[F[_]: Sync: BracketThrow] private (
    sessionPool: Resource[F, Session[F]],
    crypto: Crypto
) extends Users[F] {

  import UserQueries._

  def find(userName: UserName): F[Option[User]] = sessionPool.use { session =>
    session.prepare(selectUser).use(q => q.option(userName).map(_.map(_._1)))
  }

  def authorize(
      username: UserName,
      password: Password
  ): F[Option[User]] =
    sessionPool.use { session =>
      session.prepare(selectUser).use { q =>
        q.option(username).map {
          case Some(u ~ p) if p.value == crypto.encrypt(password).value =>
            u.some
          case _ => none[User]
        }
      }
    }

  def create(
      username: UserName,
      password: Password
  ): F[UserId] = {
    sessionPool.use { session =>
      session.prepare(insertUser).use { cmd =>
        genCoercUUID[F, UserId].flatMap { id =>
          cmd
            .execute(User(id, username) ~ crypto.encrypt(password))
            .as(id)
            .handleErrorWith { case SqlState.UniqueViolation(_) =>
              UsernameInUse(username).raiseError[F, UserId]
            }
        }
      }
    }
  }
}

object LiveUsers {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]],
      crypto: Crypto
  ): F[Users[F]] = Sync[F].delay(new LiveUsers(sessionPool, crypto))
}

private object UserQueries {

  val codec: Codec[User ~ EncryptedPassword] =
    (
      uuid.cimap[UserId] ~ varchar.cimap[UserName] ~
        varchar.cimap[EncryptedPassword]
    ).imap { case i ~ n ~ p =>
      User(i, n) ~ p
    } { case u ~ p =>
      u.id ~ u.name ~ p
    }

  val selectUser: Query[UserName, User ~ EncryptedPassword] =
    sql"""
      SELECT * FROM users
      WHERE name = ${varchar.cimap[UserName]}
    """.query(codec)

  val insertUser: Command[User ~ EncryptedPassword] =
    sql"""
      INSERT INTO users
      VALUES ($codec)
    """.command
}
