package routes

import cats.effect.IO
import http._
import http.routes.UserRoutes
import io.circe.Json
import io.circe.syntax.EncoderOps
import model.user.{Password, UserName}
import org.http4s.Method.POST
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Request, Status}
import utils.Arbitraries._
import utils.mocks.auth.{defaultSuccessToken, failingAuth, successfulAuth}
import utils.suite.HttpTestSuite

class UserRoutesSpec extends HttpTestSuite {

  test("create user success") {
    forAll { (username: UserName, password: Password) =>
      {
        val routes = new UserRoutes[IO](successfulAuth).routes
        assertHttp(
          routes,
          Request(POST, uri"/auth/users")
            .withEntity(
              Json.obj(
                ("username", Json.fromString(username.value)),
                ("password", Json.fromString(password.value))
              )
            )
        ) { response =>
          response.asJson.map { json =>
            assert(
              response.status === Status.Created && json.dropNullValues === defaultSuccessToken.asJson
            )
          }
        }
      }
    }
  }

  test("create user duplicate name fail") {
    forAll { (username: UserName, password: Password) =>
      {
        val routes = new UserRoutes[IO](failingAuth).routes
        assertHttp(
          routes,
          Request(POST, uri"/auth/users")
            .withEntity(
              Json.obj(
                ("username", Json.fromString(username.value)),
                ("password", Json.fromString(password.value))
              )
            )
        ) { response =>
          response.asJson.map { json =>
            assert(
              response.status === Status.Conflict
                && json.dropNullValues === username.value.toLowerCase.asJson
            )
          }
        }
      }
    }
  }
}
