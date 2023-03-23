package mocks

import cats.effect.IO
import effects.Background

import scala.concurrent.duration.FiniteDuration

object background {
  val NoOpBackground: Background[IO] =
    new Background[IO] {
      def schedule[A](
          fa: IO[A],
          duration: FiniteDuration
      ): IO[Unit] = IO.unit
    }
}
