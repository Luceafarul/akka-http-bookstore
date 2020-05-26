package views

import models.{Book, Category}
import scalatags.Text.all._
import services.CurrencyService

object BookSearchView {
  def view(
            categories: Seq[Category],
            currencies: Seq[String],
            books: Seq[Book],
            currency: String = CurrencyService.baseCurrency): String = html(
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
      ),

      ul(
        books.map(book => li(s"${book.title} -- ${formatPrice(book.price, currency)}"))
      )
    )
  ).toString()

  private def formatPrice(price: Double, currency: String): String =
    s"${currencySymbol(currency)}%.2f".format(price)

  private val currencySymbol = Map("USD" -> '$', "EUR" -> '\u20ac')
}
