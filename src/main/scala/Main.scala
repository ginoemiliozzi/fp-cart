import cats.effect._
import cats.syntax.all._
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import eu.timepit.refined.types.numeric.PosInt
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import model.cart.ShoppingCartExpiration
import modules.{Algebras, CheckoutConfig, HttpApi, HttpClients, Programs}
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Main extends IOApp {

  implicit val logger = Slf4jLogger.getLogger[IO]
  override def run(args: List[String]): IO[ExitCode] = {

    val checkoutConfig = CheckoutConfig(PosInt(3), 1.second)
    AppResources.make[IO]().use { res =>
      for {
        algebras <- Algebras.make(
          res.redis,
          res.psql,
          ShoppingCartExpiration(FiniteDuration(1, "hour"))
        )
        clients <- HttpClients.make[IO](res.httpClient)
        programs <- Programs.make[IO](checkoutConfig, algebras, clients)
        httpApi <- HttpApi.make[IO](algebras, programs)
        _ <- BlazeServerBuilder[IO](ExecutionContext.global)
          .bindHttp(8080, "0.0.0.0")
          .withHttpApp(httpApi.httpApp)
          .serve
          .compile
          .drain
      } yield ExitCode.Success
    }

  }
}
