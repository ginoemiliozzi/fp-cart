import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import skunk.Session
import natchez.Trace.Implicits.noop // needed for skunk

final class AppResources[F[_]](
    psql: Resource[F, Session[F]],
    redis: Resource[F, RedisCommands[F, String, String]]
)

object AppResources {
  def make[F[_]: ConcurrentEffect: ContextShift: Log]()
      : Resource[F, AppResources[F]] = {

    def mkDatabaseResource: Resource[F, Resource[F, Session[F]]] =
      Session
        .pooled[F](
          host = "localhost",
          port = 5432,
          user = "postgres",
          database = "store",
          max = 10
        )

    def mkRedisResource
        : Resource[F, Resource[F, RedisCommands[F, String, String]]] =
      Resource.pure(Redis[F].utf8("redis://localhost"))

    (
      mkDatabaseResource,
      mkRedisResource
    ).mapN { case (db, redis) => new AppResources[F](db, redis) }
  }
}
