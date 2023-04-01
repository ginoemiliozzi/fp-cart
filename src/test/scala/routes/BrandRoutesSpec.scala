package routes

import cats.effect.IO
import http._
import http.routes.BrandRoutes
import io.circe.syntax.EncoderOps
import model.brand.Brand
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Status}
import utils.Arbitraries._
import utils.mocks.brands.usingBrands
import utils.suite.HttpTestSuite

final class BrandRoutesSpec extends HttpTestSuite {

  test("get brands") {
    forAll { (brands: List[Brand]) =>
      val brandRoutes = new BrandRoutes[IO](usingBrands(brands)).routes
      assertHttp(
        brandRoutes,
        Request(method = Method.GET, uri = uri"/brands")
      ) { response =>
        response.asJson.map { json =>
          val jsonResp = json.dropNullValues
          val expected = brands.asJson.dropNullValues
          assert(response.status === Status.Ok && jsonResp === expected)
        }
      }
    }
  }
}
