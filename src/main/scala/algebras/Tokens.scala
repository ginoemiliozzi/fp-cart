package algebras

import cats.effect.Sync
import cats.implicits._
import config.model.{AppConfig, JwtSecretKeyConfig, TokenExpiration}
import dev.profunktor.auth.jwt.{JwtSecretKey, JwtToken, jwtEncode}
import io.circe.syntax.EncoderOps
import model.user.UserId
import pdi.jwt.{JwtAlgorithm, JwtClaim}
import http._

import java.time.Clock

trait Tokens[F[_]] {
  def create(userId: UserId): F[JwtToken]
}

private class LiveTokens[F[_]: Sync](
    tokenSecret: JwtSecretKeyConfig,
    exp: TokenExpiration
) extends Tokens[F] {

  implicit val clock: Clock = java.time.Clock.systemUTC
  override def create(userId: UserId): F[JwtToken] = for {
    claim <- Sync[F].delay(
      JwtClaim(userId.asJson.noSpaces).issuedNow.expiresIn(exp.value.toMillis)
    )
    secretKey = JwtSecretKey(tokenSecret.value.value.value)
    token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
  } yield token
}

object LiveTokens {
  def make[F[_]: Sync](appConfig: AppConfig): F[Tokens[F]] = Sync[F].delay(
    new LiveTokens(appConfig.tokenConfig, appConfig.tokenExpiration)
  )
}
