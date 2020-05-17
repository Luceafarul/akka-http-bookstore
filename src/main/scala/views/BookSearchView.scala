package views

import models.Category
import scalatags.Text.all._

object BookSearchView {
  def view(categories: Seq[Category], currencies: Seq[String]): String = html(
    head(

    ),
    body(
      h1("Search our Awesome Catalog!"),
      form(action := "/books", method := "POST")(
        p(input(name := "title", placeholder := "Title")),
        p(input(name := "releaseDate", placeholder := "Release Date")),
        p(input(name := "author", placeholder := "Author")),
        p(select(name := "categoryId")(
          option,
          categories.map(c => option(value := c.id.get)(c.title))
        )),
        p(select(name := "currency")(currencies.map(c => option(c)))),
        p(input(`type` := "submit", value := "Submit")),
      )
    )
  ).toString()
}
