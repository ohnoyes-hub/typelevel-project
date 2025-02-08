package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.Html.*
import cats.effect.IO

final case class ForgotPasswordPage() extends Page {
    // API
    override def initCmd: Cmd[IO, Page.Msg] = 
        Cmd.None
    override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = 
        (this, Cmd.None)
    override def view(): Html[Page.Msg] = 
        div(
            h1("Forgot Password Page - TODO")
        )
} 
