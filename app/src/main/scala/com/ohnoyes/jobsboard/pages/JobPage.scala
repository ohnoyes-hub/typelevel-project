package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.Html.*
import cats.effect.IO

import com.ohnoyes.jobsboard.*

final case class JobPage(id: String) extends Page {
    // API
    override def initCmd: Cmd[IO, App.Msg] = 
        Cmd.None
    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = 
        (this, Cmd.None)
    override def view(): Html[App.Msg] = 
        div(s"Individal job page page $id - TODO")
} 
