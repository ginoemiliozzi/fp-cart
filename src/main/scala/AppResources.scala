import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import config.model.AppConfig
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.Log
import skunk.Session
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

final case class AppResources[F[_]](
    psql: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String],
    httpClient: Client[F]
)

object AppResources {
  def make[F[_]: ConcurrentEffect: ContextShift: Log](
      appConfig: AppConfig
  ): Resource[F, AppResources[F]] = {

    def mkDatabaseResource: Resource[F, Resource[F, Session[F]]] =
      Session
        .pooled[F](
          host = appConfig.postgreSQL.host.value,
          port = appConfig.postgreSQL.port.value,
          user = appConfig.postgreSQL.user.value,
          database = appConfig.postgreSQL.database.value,
          max = appConfig.postgreSQL.max.value
        )

    def mkRedisResource: Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(appConfig.redis.uri.value.value)

    def mkHttpClientResource: Resource[F, Client[F]] =
      BlazeClientBuilder[F](ExecutionContext.global)
        .withConnectTimeout(appConfig.httpClientConfig.connectTimeout)
        .withRequestTimeout(appConfig.httpClientConfig.requestTimeout)
        .withDefaultSslContext
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
