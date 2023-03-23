package modules

import algebras.auth.{Auth, LiveAdminAuth, LiveAuth, LiveUsersAuth, UsersAuth}
import algebras.{
  Brands,
  Categories,
  HealthCheck,
  Items,
  LiveBrands,
  LiveCategories,
  LiveCrypto,
  LiveHealthCheck,
  LiveItems,
  LiveOrders,
  LiveShoppingCart,
  LiveTokens,
  LiveUsers,
  Orders,
  ShoppingCart,
  Tokens,
  Users
}
import cats.Parallel
import cats.effect._
import cats.syntax.all._
import ciris.Secret
import dev.profunktor.redis4cats.RedisCommands
import http.users.{AdminUser, CommonUser}
import model.cart.ShoppingCartExpiration
import model.user.{PasswordSalt, TokenExpiration}
import skunk._

import scala.concurrent.duration.DurationInt

object Algebras {

  // TODO move to config
  val cryptoSecret: PasswordSalt = PasswordSalt(Secret("fake-secret"))

  def make[F[_]: Concurrent: Parallel: Timer](
      redis: RedisCommands[F, String, String],
      sessionPool: Resource[F, Session[F]],
      cartExpiration: ShoppingCartExpiration
  ): F[Algebras[F]] =
    for {
      brands <- LiveBrands.make[F](sessionPool)
      categories <- LiveCategories.make[F](sessionPool)
      items <- LiveItems.make[F](sessionPool)
      cart <- LiveShoppingCart.make[F](items, redis, cartExpiration)
      orders <- LiveOrders.make[F](sessionPool)
      tokens <- LiveTokens.make[F]()
      crypto <- LiveCrypto.make[F](cryptoSecret)
      users <- LiveUsers.make(sessionPool, crypto)
      authAlg <- AuthAlgebras.make(tokens, users, redis)
      health <- LiveHealthCheck.make[F](sessionPool, redis)
    } yield new Algebras[F](
      cart,
      brands,
      categories,
      items,
      orders,
      authAlg,
      health
    )
}

final class Algebras[F[_]] private (
    val cart: ShoppingCart[F],
    val brands: Brands[F],
    val categories: Categories[F],
    val items: Items[F],
    val orders: Orders[F],
    val authAlgebras: AuthAlgebras[F],
    val healthCheck: HealthCheck[F]
)

final class AuthAlgebras[F[_]] private (
    val auth: Auth[F],
    val userAuth: UsersAuth[F, CommonUser],
    val adminAuth: UsersAuth[F, AdminUser]
)

object AuthAlgebras {
  def make[F[_]: Sync](
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String]
  ): F[AuthAlgebras[F]] = {
    for {
      auth <- LiveAuth.make[F](TokenExpiration(1.day), tokens, users, redis)
      userAuth <- LiveUsersAuth.make[F](redis)
      adminAuth <- LiveAdminAuth.make[F]()
    } yield new AuthAlgebras(auth, userAuth, adminAuth)
  }
}
