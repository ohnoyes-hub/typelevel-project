package com.ohnoyes.jobsboard

import tyrian.* //TyrianApp
import tyrian.Html.* //Html
import cats.effect.* 
import scala.scalajs.js.annotation.*
import org.scalajs.dom.{document,console}
import scala.concurrent.duration.*
import tyrian.cmds.Logger
import org.scalajs.dom.window

import core.*
import components.*

object App {
    type Msg = Router.Msg

    case class Model(router: Router)
}

@JSExportTopLevel("OhNoYesApp")
class App extends TyrianApp[App.Msg, App.Model]{
    import App.*
    
    override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
        val (router, cmd) = Router.startAt(window.location.pathname)
        (Model(router), cmd)
    }
        // (Model(Router.startAt(window.location.pathname)), Cmd.None)    

    override def subscriptions(model: Model): Sub[IO, Msg] =  
        Sub.make( // listener for browser history changes
            "urlChange", model.router.history.state.discrete // fs2.Stream of locations
            .map(_.get) // fs2.Stream of Option[String]
            .map(newLocation => Router.ChangeLocation(newLocation, true))    
        )

    override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = { 
        case msg: Router.Msg => 
            val (newRouter, cmd) = model.router.update(msg)
            (Model(router = newRouter), cmd)
        case _ =>  (model, Cmd.None) // TODO to check external redirects as weel
    }

    override def view(model: Model): Html[Msg] = // virtual dom
        div(
            Header.view(),
            div(s"Now at location: ${model.router.location}")    
        )
}
