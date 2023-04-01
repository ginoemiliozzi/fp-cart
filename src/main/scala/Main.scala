import cats.effect._
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import modules.{Algebras, HttpApi, HttpClients, Programs, Security}
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  implicit val logger = Slf4jLogger.getLogger[IO]
  override def run(args: List[String]): IO[ExitCode] = {

    config.loader[IO].flatMap { appConfig =>
      Logger[IO].info(s"Loaded config: $appConfig") *>
        AppResources.make[IO](appConfig).use { res =>
          for {
            algebras <- Algebras.make(
              res.redis,
              res.psql,
              appConfig
            )
            clients <- HttpClients
              .make[IO](res.httpClient, appConfig.paymentConfig)
            programs <- Programs
              .make[IO](appConfig.checkoutConfig, algebras, clients)
            security <- Security.make[IO](appConfig)
            httpApi <- HttpApi.make[IO](
              algebras,
              programs,
              security.adminJwtAuth,
              security.userJwtAuth
            )
            _ <- BlazeServerBuilder[IO](ExecutionContext.global)
              .bindHttp(
                appConfig.httpServerConfig.port.value,
                appConfig.httpServerConfig.host.value
              )
              .withHttpApp(httpApi.httpApp)
              .serve
              .compile
              .drain
          } yield ExitCode.Success
        }
    }

  }
}
