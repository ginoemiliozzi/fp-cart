package utils.mocks

import algebras.auth.Auth
import cats.effect.IO
import dev.profunktor.auth.jwt
import dev.profunktor.auth.jwt.JwtToken
import http.users.InvalidUserOrPassword
import model.user

object auth {

  val defaultSuccessToken: JwtToken = JwtToken(
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.c3VjY2Vzcw.hSmFc2_O5x_6adKbg_ZWECZZiCN7rgEbmfBlZw9CM_k"
  )

  def successfulAuth: Auth[IO] = new Auth[IO] {
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

  def invalidCredentialsLogin: Auth[IO] = new Auth[IO] {
    override def newUser(
        username: user.UserName,
        password: user.Password
    ): IO[jwt.JwtToken] = IO.pure(defaultSuccessToken)

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
