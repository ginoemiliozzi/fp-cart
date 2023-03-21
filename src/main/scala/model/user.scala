package model

import ciris.Secret
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import java.util.UUID
import javax.crypto.Cipher
import scala.concurrent.duration.FiniteDuration

object user {
  @newtype case class UserId(value: UUID)
  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)
  @newtype case class EncryptedPassword(value: String)
  case class User(id: UserId, name: UserName)

  // move to config
  @newtype case class PasswordSalt(value: Secret[NonEmptyString])
  @newtype case class EncryptCipher(value: Cipher)
  @newtype case class DecryptCipher(value: Cipher)
  @newtype case class TokenExpiration(value: FiniteDuration)
}
