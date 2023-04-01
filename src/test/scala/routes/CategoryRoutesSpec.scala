package routes

import cats.effect.IO
import http._
import http.routes.CategoryRoutes
import io.circe.syntax.EncoderOps
import model.category.Category
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Status}
import utils.Arbitraries._
import utils.mocks.categories.usingCategories
import utils.suite.HttpTestSuite

final class CategoryRoutesSpec extends HttpTestSuite {

  test("get categories") {
    forAll { (categories: List[Category]) =>
      assertHttp(
        new CategoryRoutes[IO](usingCategories(categories)).routes,
        Request(method = Method.GET, uri = uri"/categories")
      ) { response =>
        response.asJson.map { json =>
          val jsonResp = json.dropNullValues
          val expected = categories.asJson.dropNullValues
          assert(response.status === Status.Ok && jsonResp === expected)
        }
      }
    }
  }
}
