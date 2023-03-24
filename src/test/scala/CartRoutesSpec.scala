import cats.effect.IO
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Status, Uri}
import suite.HttpTestSuite
import http._
import http.routes.CartRoutes
import utils.Arbitraries._
import mocks.middlewares.{commonUserAuthMiddleware, defaultUser}
import mocks.shoppingCart.successfulCart
import model.cart.{Cart, CartTotal}

final class CartRoutesSpec extends HttpTestSuite {

  test("get cart for user - GET ") {
    forAll { (cartTotal: CartTotal) =>
      val cartRoutes =
        new CartRoutes[IO](successfulCart(cartTotal))
          .routes(commonUserAuthMiddleware)
      assertHttp(
        cartRoutes,
        Request(method = Method.GET, uri = uri"/cart")
      ) { response =>
        response.asJson.map { json =>
          val jsonResp = json.dropNullValues
          val expected = cartTotal.asJson.dropNullValues
          assert(response.status === Status.Ok && jsonResp === expected)
        }
      }
    }
  }

  test("add items to cart - POST") {
    forAll { (cartTotal: CartTotal, cart: Cart) =>
      val cartRoutes =
        new CartRoutes[IO](successfulCart(cartTotal))
          .routes(commonUserAuthMiddleware)
      assertHttp(
        cartRoutes,
        Request(method = Method.POST, uri = uri"/cart")
          .withEntity(cart)
      ) { response =>
        IO.pure(assert(response.status === Status.Created))
      }
    }
  }

  test("modify items in cart - PUT") {
    forAll { (cartTotal: CartTotal, cart: Cart) =>
      val cartRoutes =
        new CartRoutes[IO](successfulCart(cartTotal))
          .routes(commonUserAuthMiddleware)
      assertHttp(
        cartRoutes,
        Request(method = Method.PUT, uri = uri"/cart")
          .withEntity(cart)
      ) { response =>
        IO.pure(assert(response.status === Status.Ok))
      }
    }
  }

  test("remove item from user cart - DELETE") {
    forAll { (cartTotal: CartTotal) =>
      val cartRoutes =
        new CartRoutes[IO](successfulCart(cartTotal))
          .routes(commonUserAuthMiddleware)
      assertHttp(
        cartRoutes,
        Request(
          method = Method.DELETE,
          uri = Uri
            .fromString(s"/cart/${defaultUser.value.id.value}")
            .getOrElse(fail())
        )
      ) { response =>
        IO.pure(assert(response.status === Status.NoContent))
      }
    }
  }
}
