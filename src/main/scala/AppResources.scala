import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.Log
import skunk.Session
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

final case class AppResources[F[_]](
    psql: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String],
    httpClient: Client[F]
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

    def mkRedisResource: Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8("redis://localhost")

    def mkHttpClientResource: Resource[F, Client[F]] =
      BlazeClientBuilder[F](ExecutionContext.global)
        .withConnectTimeout(3.seconds)
        .withRequestTimeout(3.seconds)
        .resource

    (
      mkDatabaseResource,
      mkRedisResource,
      mkHttpClientResource
    ).mapN { case (db, redis, httpClient) =>
      new AppResources[F](db, redis, httpClient)
    }
  }
}
