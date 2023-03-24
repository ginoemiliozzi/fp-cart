import cats.effect.IO
import http.routes.BrandRoutes
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import mocks.brands.usingBrands
import model.brand.Brand
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Status}
import suite.HttpTestSuite
import http._
import utils.Arbitraries._

final class BrandRoutesSpec extends HttpTestSuite {

  test("get brands") {
    forAll { (brands: List[Brand]) =>
      val brandRoutes = new BrandRoutes[IO](usingBrands(brands)).routes
      assertHttp(
        brandRoutes,
        Request(method = Method.GET, uri = uri"/brands")
      ) { response =>
        val bodyJson = response.asJson
        bodyJson.map { json =>
          assert(
            response.status === Status.Ok && json.dropNullValues === brands.asJson.dropNullValues
          )
        }
      }
    }
  }
}
