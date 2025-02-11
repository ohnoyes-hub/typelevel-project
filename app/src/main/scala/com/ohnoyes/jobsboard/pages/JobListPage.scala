package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.Html.*
import cats.effect.IO

import com.ohnoyes.jobsboard.*


final case class JobListPage() extends Page {
    // API
    override def initCmd: Cmd[IO, App.Msg] = 
        Cmd.None
    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = 
        (this, Cmd.None)
    override def view(): Html[App.Msg] = 
        div("Job list page page - TODO")
} 
