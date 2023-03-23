package suite

import cats.effect.{ContextShift, IO, Timer}
import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.util.UUID
import scala.concurrent.ExecutionContext

trait PureTestSuite extends AnyFunSuite with ScalaCheckDrivenPropertyChecks {

  // implicits needed for Cats Retry and Skunk
  implicit val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  def spec(testName: String)(f: => IO[Assertion]): Unit =
    test(s"$testName - ${UUID.randomUUID}")(IO.suspend(f).unsafeToFuture())

}
