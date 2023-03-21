package http

import effects.MonadThrow
import io.circe.Decoder
import org.http4s.{Request, Response}
import org.http4s.circe.{JsonDecoder, toMessageSynax}
import org.http4s.dsl.Http4sDsl
import cats.syntax.all._
import eu.timepit.refined.api.Validate
import eu.timepit.refined.predicates.all.Size

private[http] trait refinedDecoder {

  implicit class RefinedRequestDecoder[F[_]: JsonDecoder: MonadThrow](
      req: Request[F]
  ) extends Http4sDsl[F] {

    def decodeR[A: Decoder](
        f: A => F[Response[F]]
    ): F[Response[F]] =
      req.asJsonDecode[A].attempt.flatMap {
        case Left(e) =>
          Option(e.getCause) match {
            case Some(c) if c.getMessage.startsWith("Predicate") =>
              BadRequest(c.getMessage)
            case _ =>
              UnprocessableEntity()
          }
        case Right(a) => f(a)
      }
  }

  implicit def validateSizeN[N <: Int, R](implicit
      w: ValueOf[N]
  ): Validate.Plain[R, Size[N]] =
    Validate.fromPredicate[R, Size[N]](
      _.toString.length == w.value,
      _ => s"Must have ${w.value} digits",
      Size[N](w.value)
    )
}
