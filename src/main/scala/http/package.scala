import cats.Applicative
import cats.syntax.all._
import eu.timepit.refined._
import eu.timepit.refined.api.{Refined, Validate}
import io.circe.Encoder
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.circe.jsonEncoderOf
import org.http4s.EntityEncoder

package object http extends jsonCodecs with refinedDecoder {
  implicit def coercibleQueryParamDecoder[A: Coercible[
    B,
    *
  ], B: QueryParamDecoder]: QueryParamDecoder[A] =
    QueryParamDecoder[B].map(_.coerce[A])

  implicit def refinedQueryParamDecoder[T: QueryParamDecoder, P](implicit
      ev: Validate[T, P]
  ): QueryParamDecoder[T Refined P] =
    QueryParamDecoder[T].emap(refineV[P](_).leftMap(m => ParseFailure(m, m)))

  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]
      : EntityEncoder[F, A] = jsonEncoderOf[F, A]
}
