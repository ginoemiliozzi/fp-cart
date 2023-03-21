package http

import dev.profunktor.auth.jwt.JwtSymmetricAuth
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import model.user.{Password, User, UserName}

import scala.util.control.NoStackTrace

object users {

  // Tokens
  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  // User types
  @newtype case class CommonUser(value: User)
  @newtype case class AdminUser(value: User)

  // Authentication params
  @newtype case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.value.toLowerCase)
  }

  @newtype case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value.value)
  }
  case class LoginUser(
      username: UserNameParam,
      password: PasswordParam
  )

  // Create params
  case class CreateUser(username: UserNameParam, password: PasswordParam)

  // Errors
  case class InvalidUserOrPassword(username: UserName) extends NoStackTrace
  case class UsernameInUse(username: UserName) extends NoStackTrace

}
