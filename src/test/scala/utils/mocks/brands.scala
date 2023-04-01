package utils.mocks

import algebras.Brands
import cats.effect.IO
import model.brand
import model.brand.Brand

object brands {

  def usingBrands(brands: List[Brand]): Brands[IO] = new Brands[IO] {
    override def findAll: IO[List[Brand]] = IO.pure(brands)

    override def create(name: brand.BrandName): IO[Unit] = IO.unit
  }
}
