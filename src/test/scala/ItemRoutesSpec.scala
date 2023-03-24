import cats.effect.IO
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Status, Uri}
import suite.HttpTestSuite
import http._
import http.routes.ItemRoutes
import mocks.items.usingItems
import model.brand.{Brand, BrandId, BrandName}
import model.item.Item
import utils.Arbitraries._
import utils.Generators.{cbUuid, itemsWithBrandGen}

final class ItemRoutesSpec extends HttpTestSuite {

  test("get items") {
    forAll { (items: List[Item]) =>
      val itemRoutes = new ItemRoutes[IO](usingItems(items)).routes
      assertHttp(
        itemRoutes,
        Request(Method.GET, uri"/items")
      ) { response =>
        response.asJson.map { json =>
          val jsonResp = json.dropNullValues
          val expected = items.asJson.dropNullValues
          assert(response.status === Status.Ok && jsonResp === expected)
        }
      }
    }
  }

  test("get items by brand name") {
    forAll { (items: List[Item]) =>
      val testingBrandName = "brand1"
      val itemsWithBrand = itemsWithBrandGen(
        Brand(cbUuid[BrandId].sample.get, BrandName(testingBrandName))
      ).sample.get
      val itemsToUse = items ++ itemsWithBrand
      val itemRoutes = new ItemRoutes[IO](usingItems(itemsToUse)).routes
      assertHttp(
        itemRoutes,
        Request(
          Method.GET,
          Uri.fromString(s"/items?brand=$testingBrandName").getOrElse(fail())
        )
      ) { response =>
        val bodyJson = response.asJson
        bodyJson.map { json =>
          val jsonResp = json.dropNullValues
          val expected = itemsWithBrand.asJson.dropNullValues
          assert(
            response.status === Status.Ok && jsonResp === expected
          )
        }
      }
    }
  }
}
