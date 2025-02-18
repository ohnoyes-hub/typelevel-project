package com.ohnoyes.jobsboard.pages

import tyrian.*
import tyrian.Html.* 
import tyrian.http.*
import cats.effect.*
import org.scalajs.dom.*
import scala.concurrent.duration.FiniteDuration

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.core.*

abstract class FormPage(title: String, status: Option[Page.Status]) extends Page {
    // abstract API
    protected def renderFormContent(): List[Html[App.Msg]] // for every page to override

    // public API
    override def initCmd: Cmd[IO, App.Msg] = 
        clearForm()

    override def view(): Html[App.Msg] = 
        renderForm()

    // protected API
    protected def renderForm(): Html[App.Msg] =
        div( `class` := "form-selection")(
            // Ttile: Login
            div(`class` := "top-section")(
                h1(title)
            ),
            // form
            form(name := "signin", 
            `class` := "form", 
            id := "form",
            onEvent("submit",
                e => {
                    e.preventDefault()
                    App.NoOp
                }))(
                    renderFormContent()
                ),
                    status.map(s => div(s.message)).getOrElse(div())
            )
    
    protected def renderInput(
        name: String, 
        uid: String, 
        kind: String, 
        isRequired: Boolean, 
        onChange: String => App.Msg
    ) = 
        div(`class` := "form=input")(
            label(`for` := name, `class` := "form-label")(
                if (isRequired) span("*") else span(),
                text(name)
            ),
            input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
        )
    
    protected def renderTextArea(name: String, uid: String, isRequired: Boolean, onChange: String => App.Msg) =
        div(`class` := "form=input")(
            label(`for` := name, `class` := "form-label")(
                if (isRequired) span("*") else span(),
                text(name)
            ),
            textarea(`class` := "form-control", id := uid, onInput(onChange))("")
        )
    
    protected def renderAuxLink(location: String, text: String): Html[App.Msg] =
        a(
            href := location, 
            `class` := "aux-link", 
            onEvent(
                "click", 
                e => {
                    e.preventDefault() 
                    Router.ChangeLocation(location)
                }
            )
        )(text)

    // private API
    /* 
    check if the form is loaded(if it's present on the page)
    - document.getElementById("email") != null
    check again, while the element is null, with 

    use IO effects 
     */
    private def clearForm() = 
        Cmd.Run[IO, Unit, App.Msg] {
            // IO effect
            def effect: IO[Option[HTMLFormElement]] = for {
                maybeForm <- IO(Option(document.getElementById("form").asInstanceOf[HTMLFormElement]))
                finalForm <- 
                    if (maybeForm.isEmpty) IO.sleep(FiniteDuration(100, "millis")) *> effect
                    else IO(maybeForm)
            } yield finalForm

            effect.map(_.foreach(_.reset()))
        }(_ => App.NoOp)
}

