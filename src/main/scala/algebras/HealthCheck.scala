package algebras

import algebras.health.{PostgresStatus, RedisStatus}
import cats.Parallel
import cats.effect._
import cats.effect.implicits._
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration._
import skunk._
import skunk.codec.all._
import skunk.implicits._

object health {
  @newtype case class RedisStatus(value: Boolean)

  @newtype case class PostgresStatus(value: Boolean)
}

case class AppStatus(
    redis: RedisStatus,
    postgres: PostgresStatus
)

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

final class LiveHealthCheck[
    F[_]: Concurrent: Parallel: Timer
] private (
    sessionPool: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
) extends HealthCheck[F] {

  val q: Query[Void, Int] =
    sql"SELECT pid FROM pg_stat_activity".query(int4)

  val redisHealth: F[RedisStatus] =
    redis.ping
      .map(_.nonEmpty)
      .timeout(1.second)
      .orElse(false.pure[F])
      .map(RedisStatus.apply)

  val postgresHealth: F[PostgresStatus] =
    sessionPool
      .use(_.execute(q))
      .map(_.nonEmpty)
      .timeout(1.second)
      .orElse(false.pure[F])
      .map(PostgresStatus.apply)

  def status: F[AppStatus] =
    (redisHealth, postgresHealth).parMapN(AppStatus)
}

object LiveHealthCheck {
  def make[F[_]: Concurrent: Parallel: Timer](
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[HealthCheck[F]] =
    Sync[F].delay(
      new LiveHealthCheck[F](sessionPool, redis)
    )
}
