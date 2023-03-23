import cats.effect.IO
import effects.Background
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mocks.orders.successfulOrders
import mocks.paymentClient.successfulClient
import mocks.shoppingCart.{emptyCart, successfulCart}
import model.card.Card
import model.cart.CartTotal
import model.order.{EmptyCartError, OrderId, PaymentId}
import model.user.UserId
import programs.CheckoutProgram
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import suite.PureTestSuite
import utils.Arbitraries._
import mocks.background.NoOpBackground
import utils.IOAssertion

final class CheckoutSpec extends PureTestSuite {
  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)
  implicit val logger = Slf4jLogger.getLogger[IO]

  test("successful checkout") {
    forAll {
      (
          uid: UserId,
          pid: PaymentId,
          oid: OrderId,
          ct: CartTotal,
          card: Card
      ) =>
        implicit val noOpBackground: Background[IO] = NoOpBackground

        IOAssertion {
          new CheckoutProgram[IO](
            successfulClient(pid),
            successfulCart(ct),
            successfulOrders(oid),
            retryPolicy
          ).checkout(uid, card)
            .map { id =>
              assert(id === oid)
            }
        }
    }
  }

  test("empty cart throws error") {
    forAll {
      (
          uid: UserId,
          pid: PaymentId,
          oid: OrderId,
          card: Card
      ) =>
        implicit val noOpBackground: Background[IO] = NoOpBackground

        IOAssertion {
          new CheckoutProgram[IO](
            successfulClient(pid),
            emptyCart,
            successfulOrders(oid),
            retryPolicy
          ).checkout(uid, card)
            .attempt
            .map {
              case Left(error) => assert(error === EmptyCartError)
              case Right(_)    => fail("Empty cart exception was expected")
            }
        }
    }
  }
}
