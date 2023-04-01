package utils.mocks

import cats.data.Kleisli
import cats.effect.IO
import http.users.CommonUser
import model.user.{User, UserId, UserName}
import org.http4s.server.AuthMiddleware

import java.util.UUID

object middlewares {

  val defaultUser: CommonUser =
    CommonUser(
      User(
        UserId(UUID.randomUUID),
        UserName("user")
      )
    )

  val commonUserAuthMiddleware: AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli.pure(defaultUser))

}
