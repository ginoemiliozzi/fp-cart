package modules

import cats.effect._
import cats.syntax.all._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.JwtAuth
import http.routes.admin.{
  AdminBrandRoutes,
  AdminCategoryRoutes,
  AdminItemRoutes
}
import http.routes.{
  BrandRoutes,
  CartRoutes,
  CategoryRoutes,
  CheckoutRoutes,
  HealthRoutes,
  ItemRoutes,
  OrderRoutes,
  UserRoutes
}
import http.routes.auth.{LoginRoutes, LogoutRoutes}
import http.users.{AdminUser, CommonUser}
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware._
import org.http4s.server.Router
import pdi.jwt.JwtAlgorithm

import scala.concurrent.duration._

object HttpApi {
  def make[F[_]: Concurrent: Timer](
      algebras: Algebras[F],
      programs: Programs[F]
  ): F[HttpApi[F]] =
    Sync[F].delay(
      new HttpApi[F](
        algebras,
        programs
      )
    )
}

final class HttpApi[F[_]: Concurrent: Timer] private (
    algebras: Algebras[F],
    programs: Programs[F]
) {

  // TODO move keys to config
  private def jwtAuth(secretKey: String) = JwtAuth
    .hmac(
      secretKey,
      JwtAlgorithm.HS256
    )

  private val usersMiddleware =
    JwtAuthMiddleware[F, CommonUser](
      jwtAuth("secret-user"),
      algebras.authAlgebras.userAuth.findUser
    )
  private val adminMiddleware =
    JwtAuthMiddleware[F, AdminUser](
      jwtAuth("secret-admin"),
      algebras.authAlgebras.adminAuth.findUser
    )

  private val loginRoutes =
    new LoginRoutes[F](algebras.authAlgebras.auth).routes
  private val logoutRoutes =
    new LogoutRoutes[F](algebras.authAlgebras.auth).routes(usersMiddleware)

  private val userRoutes = new UserRoutes[F](algebras.authAlgebras.auth).routes

  // Open routes
  private val healthRoutes = new HealthRoutes[F](algebras.healthCheck).routes
  private val brandRoutes = new BrandRoutes[F](algebras.brands).routes
  private val categoryRoutes = new CategoryRoutes[F](algebras.categories).routes
  private val itemRoutes = new ItemRoutes[F](algebras.items).routes

  // Secured routes
  private val cartRoutes =
    new CartRoutes[F](algebras.cart).routes(usersMiddleware)
  private val checkoutRoutes =
    new CheckoutRoutes[F](programs.checkout).routes(usersMiddleware)
  private val orderRoutes =
    new OrderRoutes[F](algebras.orders).routes(usersMiddleware)

  // Admin routes
  private val adminBrandRoutes =
    new AdminBrandRoutes[F](algebras.brands).routes(adminMiddleware)
  private val adminCategoryRoutes =
    new AdminCategoryRoutes[F](algebras.categories).routes(adminMiddleware)
  private val adminItemRoutes =
    new AdminItemRoutes[F](algebras.items).routes(adminMiddleware)

  // Combining all the http routes
  private val openRoutes: HttpRoutes[F] =
    healthRoutes <+> itemRoutes <+> brandRoutes <+>
      categoryRoutes <+> loginRoutes <+> userRoutes <+>
      logoutRoutes <+> cartRoutes <+> orderRoutes <+>
      checkoutRoutes

  private val adminRoutes: HttpRoutes[F] =
    adminItemRoutes <+> adminBrandRoutes <+> adminCategoryRoutes

  private val routes: HttpRoutes[F] = Router(
    "/v1" -> openRoutes,
    "/v1" + "/admin" -> adminRoutes
  )

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http, CORS.DefaultCORSConfig)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(true, true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(true, true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)

}
