package helpers

import models.Category
import repositories.CategoryRepository

import scala.concurrent.{ExecutionContext, Future}

class CategorySpecHelper(categoryRepository: CategoryRepository)(implicit executor: ExecutionContext) {
  val category = Category(None, "Test category")

  def createAndDelete[T](category: Category = category)(assertion: Category => Future[T]): Future[T] = {
    categoryRepository.create(category).flatMap { c =>
      val assertions = assertion(c)
      categoryRepository.delete(c.id.get).flatMap { _ => assertions }
    }
  }
}
