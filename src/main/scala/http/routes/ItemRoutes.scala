package http.routes

import algebras.Items
import cats.{Defer, Monad}
import model.brand.BrandParam
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import http._
import model.item.ItemId

final class ItemRoutes[F[_]: Monad: Defer](items: Items[F])
    extends Http4sDsl[F] {
  private[routes] val prefixPath = "/items"

  object BrandQueryParam
      extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? BrandQueryParam(brand) =>
      Ok(brand.fold(items.findAll)(b => items.findBy(b.toDomain)))

    case GET -> Root / UUIDVar(uuid) =>
      Ok(items.findById(ItemId(uuid)))
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
