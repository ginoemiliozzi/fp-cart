import cats.effect.IO
import cats.effect.concurrent.Ref
import effects.Background
import io.chrisdavenport.log4cats.Logger
import mocks.orders.{failingOrders, successfulOrders}
import mocks.paymentClient.{
  failNTimesBeforeSuccess,
  successfulPaymentClient,
  unreachableClient
}
import mocks.shoppingCart.{emptyCart, successfulCart}
import model.card.Card
import model.cart.CartTotal
import model.order.{
  EmptyCartError,
  OrderError,
  OrderId,
  PaymentError,
  PaymentId
}
import model.user.UserId
import programs.CheckoutProgram
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import suite.PureTestSuite
import utils.Arbitraries._
import mocks.background.{NoOpBackground, bgCountSchedules}
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
            successfulPaymentClient(pid),
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
            successfulPaymentClient(pid),
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

  test("payment client unreacheable") {
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
              paymentClient = unreachableClient,
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

  test("payment client succeeds on retry") {
    forAll {
      (
          uid: UserId,
          cart: CartTotal,
          pid: PaymentId,
          oid: OrderId,
          card: Card
      ) =>
        implicit val noOpBackground: Background[IO] = NoOpBackground
        val paymentClientFailingTimes = 1
        IOAssertion {
          Ref.of[IO, List[String]](List.empty).flatMap { errorsRef =>
            Ref.of[IO, Int](0).flatMap { attemptsRef =>
              implicit val logAccumulator: Logger[IO] = accErrors(errorsRef)
              new CheckoutProgram[IO](
                paymentClient = failNTimesBeforeSuccess(
                  paymentClientFailingTimes,
                  attemptsRef,
                  pid
                ),
                successfulCart(cart),
                successfulOrders(oid),
                retryPolicy
              ).checkout(uid, card)
                .attempt
                .flatMap {
                  case Left(e) =>
                    fail(s"Expected to succeed after retrying ${e.getMessage}")
                  case Right(orderId) =>
                    errorsRef.get.flatMap { errors =>
                      attemptsRef.get.map { attempts =>
                        assert(orderId === oid)
                        assert(
                          errors.size + 1 === attempts
                        ) // N errors + 1 success
                      }
                    }
                }
            }
          }
        }
    }
  }

  test("retry order creation in background") {
    forAll {
      (
          uid: UserId,
          cart: CartTotal,
          pid: PaymentId,
          card: Card
      ) =>
        IOAssertion {
          Ref.of[IO, Int](0).flatMap { schedulesRef =>
            implicit val noLogger: Logger[IO] = noLogs
            implicit val countSchedulesBg: Background[IO] =
              bgCountSchedules(schedulesRef)
            new CheckoutProgram[IO](
              successfulPaymentClient(pid),
              successfulCart(cart),
              failingOrders,
              retryPolicy
            ).checkout(uid, card)
              .attempt
              .flatMap {
                case Left(OrderError(_)) =>
                  schedulesRef.get.map { bgSchedules =>
                    assert(bgSchedules === 1)
                  }
                case _ => fail("Expected order failure and retry schedule")
              }
          }
        }
    }
  }
}
