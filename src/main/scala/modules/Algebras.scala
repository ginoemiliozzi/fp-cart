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
import cats.{ApplicativeError, Parallel}
import cats.effect._
import cats.syntax.all._
import config.model.AppConfig
import io.circe.parser.{decode => jsonDecode}
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken, jwtDecode}
import dev.profunktor.redis4cats.RedisCommands
import http.users.{AdminUser, CommonUser}
import model.auth.{AdminJwtAuth, ClaimContent}
import model.user.{User, UserId, UserName}
import pdi.jwt.JwtAlgorithm
import skunk._


object Algebras {

  def make[F[_]: Concurrent: Parallel: Timer](
      redis: RedisCommands[F, String, String],
      sessionPool: Resource[F, Session[F]],
      appConfig: AppConfig
  ): F[Algebras[F]] =
    for {
      brands <- LiveBrands.make[F](sessionPool)
      categories <- LiveCategories.make[F](sessionPool)
      items <- LiveItems.make[F](sessionPool)
      cart <- LiveShoppingCart.make[F](items, redis, appConfig.cartExpiration)
      orders <- LiveOrders.make[F](sessionPool)
      tokens <- LiveTokens.make[F](appConfig)
      crypto <- LiveCrypto.make[F](appConfig.passwordSalt)
      users <- LiveUsers.make(sessionPool, crypto)
      authAlg <- AuthAlgebras.make(tokens, users, redis, appConfig)
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
      redis: RedisCommands[F, String, String],
      appConfig: AppConfig
  ): F[AuthAlgebras[F]] = {

    // There is only one admin user
    val adminToken = JwtToken(
      appConfig.adminJwtConfig.adminToken.value.value.value
    )
    val adminJwtAuth: AdminJwtAuth =
      AdminJwtAuth(
        JwtAuth
          .hmac(
            appConfig.adminJwtConfig.secretKey.value.value.value,
            JwtAlgorithm.HS256
          )
      )
    for {
      adminClaim <- jwtDecode[F](adminToken, adminJwtAuth.value)
      content <- ApplicativeError[F, Throwable].fromEither(
        jsonDecode[ClaimContent](adminClaim.content)
      )
      adminUser = AdminUser(User(UserId(content.uuid), UserName("admin")))
      auth <- LiveAuth.make[F](appConfig.tokenExpiration, tokens, users, redis)
      userAuth <- LiveUsersAuth.make[F](redis)
      adminAuth <- LiveAdminAuth.make[F](adminToken, adminUser)
    } yield new AuthAlgebras(auth, userAuth, adminAuth)
  }
}
