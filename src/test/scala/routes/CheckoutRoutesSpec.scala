package routes

import cats.effect.IO
import http._
import http.routes.CheckoutRoutes
import io.circe.Json
import model.cart.CartTotal
import model.order.{OrderId, PaymentId}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Header, Method, Request, Status}
import programs.CheckoutProgram
import retry.RetryPolicies.limitRetries
import utils.Arbitraries._
import utils.mocks.logger.noLogs
import utils.mocks.middlewares.commonUserAuthMiddleware
import utils.mocks.orders.successfulOrders
import utils.mocks.paymentClient.successfulPaymentClient
import utils.mocks.shoppingCart.successfulCart
import utils.suite.HttpTestSuite

final class CheckoutRoutesSpec extends HttpTestSuite {

  implicit val logger = noLogs

  test("checkout - POST") {

    forAll { (cartTotal: CartTotal, paymentId: PaymentId, orderId: OrderId) =>
      val program = new CheckoutProgram[IO](
        successfulPaymentClient(paymentId),
        successfulCart(cartTotal),
        successfulOrders(orderId),
        limitRetries[IO](3)
      )
      val cartRoutes =
        new CheckoutRoutes[IO](program)
          .routes(commonUserAuthMiddleware)
      assertHttp(
        cartRoutes,
        Request(method = Method.POST, uri = uri"/checkout")
          .withHeaders(
            Header("Authorization", "Bearer eyTokenFake.fakeclaim.fakesign")
          )
          .withEntity(
            Json.obj(
              ("name", Json.fromString("cardName")),
              ("number", Json.fromLong(1234545623562563L)),
              ("expiration", Json.fromInt(1225)),
              ("cvv", Json.fromInt(123))
            )
          )
      ) { response =>
        IO.pure(assert(response.status === Status.Created))
      }
    }
  }

}
