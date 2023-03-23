package mocks

import cats.effect.IO
import cats.effect.concurrent.Ref
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

  def bgCountSchedules(
      schedulesCountRef: Ref[IO, Int]
  ): Background[IO] =
    new Background[IO] {
      def schedule[A](
          fa: IO[A],
          duration: FiniteDuration
      ): IO[Unit] = schedulesCountRef.update(_ + 1)
    }
}
