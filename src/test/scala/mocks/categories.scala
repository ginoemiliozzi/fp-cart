package mocks

import algebras.Categories
import cats.effect.IO
import model.category
import model.category.Category

object categories {

  def usingCategories(categories: List[Category]): Categories[IO] =
    new Categories[IO] {
      override def findAll: IO[List[Category]] = IO.pure(categories)
      override def create(name: category.CategoryName): IO[Unit] = IO.unit
    }
}
