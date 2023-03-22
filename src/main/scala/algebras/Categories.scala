package algebras

import cats.effect.{Resource, Sync}
import cats.implicits._
import effects.BracketThrow
import model.category.{Category, CategoryId, CategoryName}
import model._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import utils.Utils.genCoercUUID

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[Unit]
}

object LiveCategories {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Categories[F]] =
    Sync[F].delay(
      new LiveCategories[F](sessionPool)
    )
}

final class LiveCategories[F[_]: Sync: BracketThrow] private (
    sessionPool: Resource[F, Session[F]]
) extends Categories[F] {
  import CategoryQueries._

  def findAll: F[List[Category]] =
    sessionPool.use(_.execute(selectAll))

  def create(name: CategoryName): F[Unit] =
    sessionPool.use { session =>
      session.prepare(insertCategory).use { cmd =>
        genCoercUUID[F, CategoryId].flatMap { id =>
          cmd.execute(Category(id, name)).void
        }
      }
    }
}

private object CategoryQueries {

  val categoryCodec: Codec[Category] =
    (uuid.cimap[CategoryId] ~ varchar.cimap[CategoryName]).imap { case i ~ n =>
      Category(i, n)
    }(c => c.uuid ~ c.name)

  val selectAll: Query[Void, Category] =
    sql"""
      SELECT * FROM categories
    """.query(categoryCodec)

  val insertCategory: Command[Category] =
    sql"""
      INSERT INTO categories
      VALUES ($categoryCodec)
    """.command
}
