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
import pages.*

object App {
    trait Msg
    case object NoOp extends Msg

    case class Model(router: Router, session: Session, page: Page)
}

@JSExportTopLevel("OhNoYesApp")
class App extends TyrianApp[App.Msg, App.Model]{
    import App.*
    
    override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
        val location = window.location.pathname
        val page = Page.get(location) // factory
        val pageCmd = page.initCmd
        val (router, routerCmd) = Router.startAt(location)
        val session = Session()
        val sessionCmd = session.initCmd
        (Model(router, session, page), routerCmd |+| sessionCmd |+| pageCmd)
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
            val (newRouter, routerCmd) = model.router.update(msg)
            if (model.router == newRouter) // no change necessary
                (model, Cmd.None)
            else { // location change, need to re-render appropriate page
                val newPage = Page.get(newRouter.location)
                val newPageCmd = newPage.initCmd
                (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)
            }
        case msg: Session.Msg => 
            val (newSession, sessionCmd) = model.session.update(msg)
            (model.copy(session = newSession), sessionCmd)
        case msg: App.Msg =>
            // update the page
            val (newPage, cmd) = model.page.update(msg)
            (model.copy(page = newPage), cmd)
        //case _ =>  (model, Cmd.None) // TODO to check external redirects as weel
    }

    override def view(model: Model): Html[Msg] = // virtual dom
        div(
            Header.view(),
            main(
                div(`class` := "container-fluid")(
                    model.page.view()
                )
            )
        )
}
