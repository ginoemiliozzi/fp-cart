package utils.mocks
import algebras.auth.AuthCreds
import cats.effect.IO
import dev.profunktor.auth.jwt
import dev.profunktor.auth.jwt.JwtToken
import http.users.{InvalidUserOrPassword, UsernameInUse}
import model.user

object auth {

  val defaultSuccessToken: JwtToken = JwtToken(
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.c3VjY2Vzcw.hSmFc2_O5x_6adKbg_ZWECZZiCN7rgEbmfBlZw9CM_k"
  )

  def successfulAuth: AuthCreds[IO] = new AuthCreds[IO] {
    override def newUser(
        username: user.UserName,
        password: user.Password
    ): IO[jwt.JwtToken] = IO.pure(defaultSuccessToken)

    override def login(
        username: user.UserName,
        password: user.Password
    ): IO[jwt.JwtToken] = IO.pure(defaultSuccessToken)

    override def logout(
        token: jwt.JwtToken,
        username: user.UserName
    ): IO[Unit] = IO.unit
  }

  def failingAuth: AuthCreds[IO] = new AuthCreds[IO] {
    override def newUser(
        username: user.UserName,
        password: user.Password
    ): IO[jwt.JwtToken] = IO.raiseError(UsernameInUse(username))

    override def login(
        username: user.UserName,
        password: user.Password
    ): IO[jwt.JwtToken] = IO.raiseError(InvalidUserOrPassword(username))

    override def logout(
        token: jwt.JwtToken,
        username: user.UserName
    ): IO[Unit] = IO.unit
  }
}
