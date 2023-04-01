package utils.suite

import cats.effect.IO
import org.http4s._
import org.scalatest.Assertion
import utils.IOAssertion

trait HttpTestSuite extends PureTestSuite {

  def assertHttp(routes: HttpRoutes[IO], req: Request[IO])(
      assertion: Response[IO] => IO[Assertion]
  ): Unit = IOAssertion {
    routes.run(req).value.flatMap {
      case Some(resp) =>
        assertion(resp)
      case None => fail("route not found")
    }
  }

}
