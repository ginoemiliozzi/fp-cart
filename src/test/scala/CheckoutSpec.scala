import cats.effect.IO
import effects.Background
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mocks.orders.successfulOrders
import mocks.paymentClient.successfulClient
import mocks.shoppingCart.successfulCart
import model.card.Card
import model.cart.CartTotal
import model.order.{OrderId, PaymentId}
import model.user.UserId
import programs.CheckoutProgram
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import suite.PureTestSuite
import utils.Arbitraries._
import mocks.background.NoOpBackground

final class CheckoutSpec extends PureTestSuite {
  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)
  implicit val logger = Slf4jLogger.getLogger[IO]

  forAll {
    (
        uid: UserId,
        pid: PaymentId,
        oid: OrderId,
        ct: CartTotal,
        card: Card
    ) =>
      spec("successful checkout") {
        implicit val noOpBackground: Background[IO] = NoOpBackground

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
