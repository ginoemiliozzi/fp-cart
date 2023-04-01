package routes

import cats.effect.IO
import http._
import http.routes.auth.LoginRoutes
import io.circe.Json
import io.circe.syntax.EncoderOps
import model.user.{Password, UserName}
import org.http4s.Method.POST
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Request, Status}
import utils.Arbitraries._
import utils.mocks.auth.{defaultSuccessToken, invalidCredentialsLogin, successfulAuth}
import utils.suite.HttpTestSuite

class LoginRoutesSpec extends HttpTestSuite {

  test("login user success") {
    forAll { (username: UserName, password: Password) =>
      {
        val routes = new LoginRoutes[IO](successfulAuth).routes
        assertHttp(
          routes,
          Request(POST, uri"/auth/login")
            .withEntity(
              Json.obj(
                ("username", Json.fromString(username.value)),
                ("password", Json.fromString(password.value))
              )
            )
        ) { response =>
          response.asJson.map { json =>
            assert(
              response.status === Status.Ok && json.dropNullValues === defaultSuccessToken.asJson
            )
          }
        }
      }
    }
  }

  test("login user fail auth") {
    forAll { (username: UserName, password: Password) =>
      {
        val routes = new LoginRoutes[IO](invalidCredentialsLogin).routes
        assertHttp(
          routes,
          Request(POST, uri"/auth/login")
            .withEntity(
              Json.obj(
                ("username", Json.fromString(username.value)),
                ("password", Json.fromString(password.value))
              )
            )
        ) { response =>
          IO.pure(assert(response.status === Status.Forbidden))
        }
      }
    }
  }
}
