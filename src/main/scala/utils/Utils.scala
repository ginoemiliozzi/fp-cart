package utils

import cats.effect.Sync
import cats.implicits.toFunctorOps
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops.toCoercibleIdOps

import java.util.UUID

object Utils {

  def genCoercUUID[F[_]: Sync, A: Coercible[UUID, *]]: F[A] = {
    Sync[F]
      .delay(UUID.randomUUID())
      .map(uuid => uuid.coerce[A])
  }

  def readCoercUUID[F[_]: Sync, A: Coercible[UUID, *]](s: String): F[A] = {
    Sync[F]
      .delay {
        UUID.fromString(s)
      }
      .map(uuid => uuid.coerce[A])
  }
}
