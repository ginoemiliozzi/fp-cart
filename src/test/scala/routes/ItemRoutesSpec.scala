package routes

import cats.effect.IO
import http._
import http.routes.ItemRoutes
import io.circe.syntax.EncoderOps
import model.brand.{Brand, BrandId, BrandName}
import model.item.Item
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Status, Uri}
import utils.Arbitraries._
import utils.Generators.{cbUuid, itemGen, itemsWithBrandGen}
import utils.mocks.items.usingItems
import utils.suite.HttpTestSuite

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

  test("get item by id") {
    forAll { (items: List[Item]) =>
      val itemToFind = itemGen.sample.get
      val allItems = items :+ itemToFind
      val itemRoutes = new ItemRoutes[IO](usingItems(allItems)).routes
      assertHttp(
        itemRoutes,
        Request(
          Method.GET,
          Uri.fromString(s"/items/${itemToFind.uuid.value}").getOrElse(fail())
        )
      ) { response =>
        val bodyJson = response.asJson
        bodyJson.map { json =>
          val jsonResp = json.dropNullValues
          val expected = itemToFind.asJson.dropNullValues
          assert(
            response.status === Status.Ok && jsonResp === expected
          )
        }
      }
    }
  }
}
