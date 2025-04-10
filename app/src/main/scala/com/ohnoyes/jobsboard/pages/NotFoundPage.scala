package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.Html.*
import cats.effect.IO

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.common.*

final case class NotFoundPage() extends Page {
    // API
    override def initCmd: Cmd[IO, App.Msg] = 
        Cmd.None

    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = 
        (this, Cmd.None)

    override def view(): Html[App.Msg] = 
        div( `class` := "row")(
            div(`class` := "col-md-5 p-0")(
                // Left section
                div(`class` := "logo")(
                    img(src := Constants.logoImage)
                )
            ),
            div(`class` := "col-md-7")(
                div(`class` := "form-section")(
                    div(`class` := "top-section")(
                        h1(span("OH NO! ")),
                        div("Page not found!"),
                    )
                )
            )
        )
        
} 
