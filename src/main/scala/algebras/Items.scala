package algebras

import cats.effect.{Resource, Sync}
import cats.implicits._
import model.brand.{Brand, BrandId, BrandName}
import model.item.{
  CreateItem,
  Item,
  ItemDescription,
  ItemId,
  ItemName,
  UpdateItem
}
import model._
import model.category.{Category, CategoryId, CategoryName}
import skunk._
import skunk.codec.all._
import skunk.implicits._
import squants.market.USD
import utils.Utils.genCoercUUID

import java.util.UUID

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findBy(brand: BrandName): F[List[Item]]
  def findById(itemId: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[Unit]
  def update(item: UpdateItem): F[Unit]
}

final class LiveItems[F[_]: Sync] private (
    sessionPool: Resource[F, Session[F]]
) extends Items[F] {
  import ItemQueries._

  def findAll: F[List[Item]] =
    sessionPool.use(_.execute(selectAll))

  def findBy(brand: BrandName): F[List[Item]] =
    sessionPool.use { session =>
      session.prepare(selectByBrand).use { ps =>
        ps.stream(brand, 1024).compile.toList
      }
    }

  def findByLimited(brand: BrandName): F[List[Item]] =
    sessionPool.use { session =>
      session.prepare(selectByBrand).use { ps =>
        ps.cursor(brand).use(_.fetch(10))
      }
    }

  def findById(itemId: ItemId): F[Option[Item]] =
    sessionPool.use { session =>
      session.prepare(selectById).use { ps =>
        ps.option(itemId)
      }
    }

  def create(item: CreateItem): F[Unit] =
    sessionPool.use { session =>
      session.prepare(insertItem).use { cmd =>
        genCoercUUID[F, ItemId].flatMap { id =>
          cmd.execute(id ~ item).void
        }
      }
    }

  def update(item: UpdateItem): F[Unit] =
    sessionPool.use { session =>
      session.prepare(updateItem).use { cmd =>
        cmd.execute(item).void
      }
    }
}

object ItemQueries {

  val decoderJoined: Decoder[Item] =
    (
      uuid ~ varchar ~ varchar ~ numeric ~
        uuid ~ varchar ~ uuid ~ varchar
    ).map { case i ~ n ~ d ~ p ~ bi ~ bn ~ ci ~ cn =>
      Item(
        ItemId(i),
        ItemName(n),
        ItemDescription(d),
        USD(p),
        Brand(BrandId(bi), BrandName(bn)),
        Category(CategoryId(ci), CategoryName(cn))
      )
    }

  val selectAll: Query[Void, Item] =
    sql"""
      SELECT i.uuid, i.name, i.description, i.price,
      b.uuid, b.name, c.uuid, c.name
      FROM items i
      INNER JOIN brands b ON i.brand_id = b.uuid
      INNER JOIN categories c ON i.category_id = c.uuid
    """.query(decoderJoined)

  val selectByBrand: Query[BrandName, Item] =
    sql"""
      SELECT i.uuid, i.name, i.description, i.price,
      b.uuid, b.name, c.uuid, c.name
      FROM items AS i
      INNER JOIN brands AS b ON i.brand_id = b.uuid
      INNER JOIN categories AS c ON i.category_id = c.uuid
      WHERE b.name LIKE ${varchar.cimap[BrandName]}
    """.query(decoderJoined)

  val selectById: Query[ItemId, Item] =
    sql"""
      SELECT i.uuid, i.name, i.description, i.price,
      b.uuid, b.name, c.uuid, c.name
      FROM items AS i
      INNER JOIN brands AS b ON i.brand_id = b.uuid
      INNER JOIN categories AS c ON i.category_id = c.uuid
      WHERE i.uuid = ${uuid.cimap[ItemId]}
    """.query(decoderJoined)

  val insertItem: Command[ItemId ~ CreateItem] =
    sql"""
      INSERT INTO items
      VALUES ($uuid, $varchar, $varchar, $numeric, $uuid, $uuid)
    """.command.contramap { case id ~ i =>
      id.value ~ i.name.value ~ i.description.value ~
        i.price.amount ~ i.brandId.value ~ i.categoryId.value
    }

  val updateItem: Command[UpdateItem] =
    sql"""
      UPDATE items
      SET price = $numeric
      WHERE uuid = ${uuid.cimap[ItemId]}
    """.command.contramap(i => i.price.amount ~ i.id)
}
