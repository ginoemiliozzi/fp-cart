package config

import cats.effect.{Async, ContextShift}
import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import config.environment.AppEnvironment
import config.environment.AppEnvironment.{Prod, Test}
import config.model._

import scala.concurrent.duration.DurationInt

object loader {

  private def default(
      redisUri: RedisURI,
      paymentUri: PaymentURI
  ): ConfigValue[AppConfig] =
    (
      env("FP_CART_JWT_SECRET_KEY").as[NonEmptyString].secret,
      env("FP_CART_JWT_CLAIM").as[NonEmptyString].secret,
      env("FP_CART_ACCESS_TOKEN_SECRET_KEY").as[NonEmptyString].secret,
      env("FP_CART_ADMIN_USER_TOKEN").as[NonEmptyString].secret,
      env("FP_CART_PASSWORD_SALT").as[NonEmptyString].secret
    ).parMapN { (secretKey, claimStr, tokenKey, adminToken, salt) =>
      AppConfig(
        AdminJwtConfig(
          JwtSecretKeyConfig(secretKey),
          JwtClaimConfig(claimStr),
          AdminUserTokenConfig(adminToken)
        ),
        JwtSecretKeyConfig(tokenKey),
        PasswordSalt(salt),
        TokenExpiration(30.minutes),
        ShoppingCartExpiration(30.minutes),
        CheckoutConfig(
          retriesLimit = 3,
          retriesBackoff = 10.milliseconds
        ),
        PaymentConfig(paymentUri),
        HttpClientConfig(
          connectTimeout = 2.seconds,
          requestTimeout = 2.seconds
        ),
        PostgreSQLConfig(
          host = "localhost",
          port = 5432,
          user = "postgres",
          database = "store",
          max = 10
        ),
        RedisConfig(redisUri),
        HttpServerConfig(
          host = "0.0.0.0",
          port = 8080
        )
      )
    }

  def apply[F[_]: Async: ContextShift]: F[AppConfig] =
    env("FP_CART_APP_ENV")
      .as[AppEnvironment]
      .flatMap {
        case Test =>
          default(
            redisUri = RedisURI(NonEmptyString("redis://localhost")),
            paymentUri =
              PaymentURI(NonEmptyString("http://localhost:1234/fake-payments"))
          )
        case Prod =>
          default(
            redisUri = RedisURI(NonEmptyString("redis://10.123.154.176")),
            paymentUri =
              PaymentURI(NonEmptyString("https://fakepayments.net/api"))
          )
      }
      .load[F]
}
