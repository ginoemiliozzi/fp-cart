package algebras

import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeId
import dev.profunktor.auth.jwt.JwtToken

trait Tokens[F[_]] {
  def create: F[JwtToken]
}

private class LiveTokens[F[_]: Sync](
) extends Tokens[F] {
  override def create: F[JwtToken] = JwtToken("FAKE-Token").pure
}

object LiveTokens {
  def make[F[_]: Sync](): F[Tokens[F]] = Sync[F].delay(new LiveTokens())
}
