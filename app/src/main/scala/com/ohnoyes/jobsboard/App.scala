package com.ohnoyes.jobsboard

import tyrian.* //TyrianApp
import tyrian.Html.* //Html
import cats.effect.* 
import scala.scalajs.js.annotation.*
import org.scalajs.dom.{document,console}
import scala.concurrent.duration.*
import tyrian.cmds.Logger

object App {
    sealed trait Msg
    case class Increment(amount: Int) extends Msg

    case class Model(count: Int)
}

@JSExportTopLevel("OhNoYesApp")
class App extends TyrianApp[App.Msg, App.Model]{
    import App.*
    //            TyrianApp[Int,           String]{
    //                      ^^message      ^^model="state"
    /* 
        Send messages by:
            - trigger a command
            - create a subscription
            - listening for an even
     */

    // Tyrian launch function:
    override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = 
        (Model(0), Cmd.None)    

    // subscriptions are potentially endless stream of messages
    override def subscriptions(model: Model): Sub[IO, Msg] = 
        Sub.every[IO](1.second).map(_ => Increment(1))
        //Sub.None

    // model can change by receiving messages (state transition)
    // model => message => (new model, __)
    // update triggered when we get a new message
    override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = { case Increment(amount) => 
        // console.log(s"incrementing count by $amount") // not the recommended way
        (model.copy(count = model.count + amount), Logger.consoleLog[IO]("Changing count by" + amount)) 
    }

    // view triggered when whenever model changes
    override def view(model: Model): Html[Msg] = // virtual dom
        div(
            button(onClick(Increment(1)))("increase me"),
            button(onClick(Increment(-1)))("decrease me"),
            div(s"Tyrian running: ${model.count}!")
        )
}
