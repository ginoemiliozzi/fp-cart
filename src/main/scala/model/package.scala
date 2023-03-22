import cats.Eq
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import skunk.Codec

import java.util.UUID

package object model {

  // does not work as implicit for some reason...
  private def coercibleEq[A: Eq, B: Coercible[A, *]]: Eq[B] =
    new Eq[B] {
      def eqv(x: B, y: B): Boolean =
        Eq[A].eqv(x.repr.asInstanceOf[A], y.repr.asInstanceOf[A])
    }

  implicit def coercibleStringEq[B: Coercible[String, *]]: Eq[B] =
    coercibleEq[String, B]

  implicit def coercibleUuidEq[B: Coercible[UUID, *]]: Eq[B] =
    coercibleEq[UUID, B]

  implicit def coercibleIntEq[B: Coercible[Int, *]]: Eq[B] =
    coercibleEq[Int, B]

  // Skunk
  implicit class SkunkCodecOps[B](codec: Codec[B]) {
    def cimap[A: Coercible[B, *]](implicit ev: Coercible[A, B]): Codec[A] =
      codec.imap(_.coerce[A])((ev(_)))
  }

}
