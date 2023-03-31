package modules

import cats.effect.Sync
import config.model.AppConfig
import dev.profunktor.auth.jwt.{JwtAuth, JwtSymmetricAuth}
import model.auth.{AdminJwtAuth, UserJwtAuth}
import pdi.jwt.JwtAlgorithm

final class Security private (
    val adminJwtAuth: AdminJwtAuth,
    val userJwtAuth: UserJwtAuth
)

object Security {
  private def jwtAuth(secretKey: String): JwtSymmetricAuth = JwtAuth
    .hmac(
      secretKey,
      JwtAlgorithm.HS256
    )

  def make[F[_]: Sync](appConfig: AppConfig): F[Security] = Sync[F].delay {
    new Security(
      AdminJwtAuth(
        jwtAuth(appConfig.adminJwtConfig.secretKey.value.value.value)
      ),
      UserJwtAuth(
        jwtAuth(appConfig.tokenConfig.value.value.value)
      )
    )
  }
}
