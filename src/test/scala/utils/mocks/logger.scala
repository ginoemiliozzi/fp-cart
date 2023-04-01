package utils.mocks

import cats.effect.IO
import cats.effect.concurrent.Ref
import io.chrisdavenport.log4cats.Logger

object logger {

  val noLogs: Logger[IO] = new Logger[IO] {
    override def error(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def warn(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def info(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def debug(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def trace(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def error(message: => String): IO[Unit] = IO.unit
    override def warn(message: => String): IO[Unit] = IO.unit
    override def info(message: => String): IO[Unit] = IO.unit
    override def debug(message: => String): IO[Unit] = IO.unit
    override def trace(message: => String): IO[Unit] = IO.unit
  }

  def accErrors(errorsRef: Ref[IO, List[String]]): Logger[IO] = new Logger[IO] {
    override def error(t: Throwable)(message: => String): IO[Unit] =
      errorsRef.update(prev => message :: prev)
    override def warn(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def info(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def debug(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def trace(t: Throwable)(message: => String): IO[Unit] = IO.unit
    override def error(message: => String): IO[Unit] =
      errorsRef.update(prev => message :: prev)
    override def warn(message: => String): IO[Unit] = IO.unit
    override def info(message: => String): IO[Unit] = IO.unit
    override def debug(message: => String): IO[Unit] = IO.unit
    override def trace(message: => String): IO[Unit] = IO.unit
  }
}
