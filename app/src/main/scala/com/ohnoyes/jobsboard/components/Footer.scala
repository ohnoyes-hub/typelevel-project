package com.ohnoyes.jobsboard.components

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.Date

import com.ohnoyes.jobsboard.*

object Footer {
  def view(): Html[App.Msg] =
    div(`class` := "footer")(
      p(
        text("Made in "),
        a(href := "https://www.scala-lang.org/", target := "blank")("Scala"),
        text(" and "),
        a(href := "https://typelevel.org/", target := "blank")("Typelevel"),
        text(" with ❤️ by ohnoyes"), 
      ),
      // copyright
      p(s"© ${new Date().getFullYear()} ohnoyes")
    )

}
