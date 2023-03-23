import cats.effect.IO
import cats.effect.concurrent.Ref
import effects.Background
import io.chrisdavenport.log4cats.Logger
import mocks.orders.successfulOrders
import mocks.paymentClient.{successfulClient, unreachableClient}
import mocks.shoppingCart.{emptyCart, successfulCart}
import model.card.Card
import model.cart.CartTotal
import model.order.{EmptyCartError, OrderId, PaymentError, PaymentId}
import model.user.UserId
import programs.CheckoutProgram
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import suite.PureTestSuite
import utils.Arbitraries._
import mocks.background.NoOpBackground
import mocks.logger.{accErrors, noLogs}
import utils.IOAssertion

final class CheckoutSpec extends PureTestSuite {
  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  test("successful checkout") {
    forAll {
      (
          uid: UserId,
          pid: PaymentId,
          oid: OrderId,
          ct: CartTotal,
          card: Card
      ) =>
        implicit val noOpBckgrnd: Background[IO] = NoOpBackground
        implicit val noLogsLogger: Logger[IO] = noLogs
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
        implicit val noLogsLogger: Logger[IO] = noLogs
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

  test("retry on payment error") {
    forAll {
      (
          uid: UserId,
          cart: CartTotal,
          oid: OrderId,
          card: Card
      ) =>
        implicit val noOpBackground: Background[IO] = NoOpBackground
        IOAssertion {
          Ref.of[IO, List[String]](List.empty).flatMap { errorsRef =>
            implicit val logAccumulator: Logger[IO] = accErrors(errorsRef)
            new CheckoutProgram[IO](
              unreachableClient,
              successfulCart(cart),
              successfulOrders(oid),
              retryPolicy
            ).checkout(uid, card)
              .attempt
              .flatMap {
                case Left(PaymentError(_)) =>
                  errorsRef.get
                    .map { errors =>
                      val totalFails = 1 + MaxRetries
                      assert(errors.size === totalFails)
                    }
                case _ => fail("Payment error was expected")
              }
          }
        }
    }
  }
}
