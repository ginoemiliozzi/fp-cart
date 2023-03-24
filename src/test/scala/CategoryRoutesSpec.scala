import cats.effect.IO
import http.routes.CategoryRoutes
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Status}
import suite.HttpTestSuite
import http._
import mocks.categories.usingCategories
import model.category.Category
import utils.Arbitraries._

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
