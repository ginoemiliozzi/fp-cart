package model

import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.circe.Decoder
import io.estatico.newtype.macros.newtype

import java.util.UUID

object auth {

  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)

  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  @newtype case class ClaimContent(uuid: UUID)

  object ClaimContent {
    implicit val jsonDecoder: Decoder[ClaimContent] =
      Decoder.forProduct1("uuid")(ClaimContent.apply)
  }
}
