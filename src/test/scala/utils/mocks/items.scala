package utils.mocks

import algebras.Items
import cats.effect.IO
import model.brand.BrandName
import model.item.{CreateItem, Item, ItemId, UpdateItem}

object items {

  def usingItems(items: List[Item]): Items[IO] = new Items[IO] {
    override def findAll: IO[List[Item]] = IO.pure(items)
    override def findBy(brand: BrandName): IO[List[Item]] = IO.pure(
      items.filter(_.brand.name.value.toLowerCase == brand.value.toLowerCase)
    )
    override def findById(itemId: ItemId): IO[Option[Item]] =
      IO.pure(items.find(_.uuid == itemId))
    override def create(item: CreateItem): IO[Unit] = IO.unit
    override def update(item: UpdateItem): IO[Unit] = IO.unit
  }
}
