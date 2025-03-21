package com.ohnoyes.jobsboard.pages

import tyrian.*
import tyrian.Html.* 
import tyrian.http.*
import cats.effect.*
import org.scalajs.dom.*
import scala.concurrent.duration.FiniteDuration

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.common.*

abstract class FormPage(title: String, status: Option[Page.Status]) extends Page {
    // abstract API
    protected def renderFormContent(): List[Html[App.Msg]] // for every page to override

    // public API
    override def initCmd: Cmd[IO, App.Msg] = 
        clearForm()

    override def view(): Html[App.Msg] = 
        renderForm()

    //     <div class="container-fluid">
    //   <div class="row">
    //     <div class="col-md-5 p-0">
    //       <!-- ============================ PICTURE SECTION START HERE ==================================== -->
    //       <div class="logo">
    //         <img src="img/singuplogo.jpg" alt="" />
    //         <h3>Continue With JVM Job Board</h3>
    //       </div>
    //     </div>
    //     <div class="col-md-7">
    //       <!-- =================================== FORM SECTION START HERE ==================================== -->
    //       <div class="form-section">
    //         <div class="top-section">
    //           <h1>Welcome to <span>JVM Board</span></h1>
    //         </div>

    //         <!-- ================= FRIST INPUT HERE ======================== -->
    //         <div class="row">
    //           <div class="col-md-12">
    //             <div class="frist-input">
    //               <label for="exampleInputEmail1" class="form-label"
    //                 ><span>*</span>Username or Email Address</label
    //               >
    //               <input
    //                 type="text"
    //                 class="form-control"
    //                 id="exampleInputEmail1"
    //                 aria-describedby="emailHelp"
    //                 placeholder="Email"
    //               />

    // protected API
    protected def renderForm(): Html[App.Msg] =
        div( `class` := "row")(
            div(`class` := "col-md-5 p-0")(
                // Left section
                div(`class` := "logo")(
                    img(src := Constants.logoImage)
                )
            ),
            div(`class` := "col-md-7")(
                div(`class` := "form-section")(
                    // Ttile: Login
                    div(`class` := "top-section")(
                        h1(span(title)),
                        maybeRenderErrors()
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
                        )
                )
            )
        )

    protected def renderInput(
        name: String, 
        uid: String, 
        kind: String, 
        isRequired: Boolean, 
        onChange: String => App.Msg
    ) = 
        div(`class` := "row")(
            div(`class` := "col-md-12")(
                div(`class` := "form-input")(
                    label(`for` := uid, `class` := "form-label")(
                        if (isRequired) span("*") else span(),
                        text(name)
                    ),
                    input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
                )
            )
        )
    
    protected def renderToggle(
        name: String, 
        uid: String, 
        isRequired: Boolean, 
        onChange: String => App.Msg
    ) = 
        div(`class` := "row")(
            div(`class` := "col-md-6 job")(
                div(`class` := "form-check form-switch")(
                    label(`for` := uid, `class` := "form-check-label")(
                        if (isRequired) span("*") else span(),
                        text(name)
                    ),
                    input(`type` := "checkbox", `class` := "form-check-input", id := uid, onInput(onChange))
                )
            )
        )

    protected def renderImageUploadInput(
        name: String, 
        uid: String, 
        imgSrc: Option[String],
        onChange: Option[File] => App.Msg
    ) = 
        div(`class` := "row")(
            div(`class` := "col-md-12")(
                div(`class` := "form=input")(
                    label(`for` := uid, `class` := "form-label")(name),
                    input(
                        `type` := "file", 
                        `class` := "form-control", 
                        id := uid,
                        accept := "image/*",
                        // multiple := "multiple", // to select multiple files
                        onEvent("change", e => {
                            val imageInput = e.target.asInstanceOf[HTMLInputElement]
                            val fileList = imageInput.files // FileList
                            onChange(if (fileList.length > 0) Some(fileList(0)) else None)
                            }
                        )
                    )
                    // ,img(
                    //     id := "preview",
                    //     src := imgSrc.getOrElse(""),
                    //     alt := "Preview",
                    //     width := "100",
                    //     height := "100"
                    // )
                )
            )
        )
    
    protected def renderTextArea(name: String, uid: String, isRequired: Boolean, onChange: String => App.Msg) =
        div(`class` := "row")(
            div(`class` := "col-md-12")(
                div(`class` := "form=input")(
                    label(`for` := name, `class` := "form-label")(
                        if (isRequired) span("*") else span(),
                        text(name)
                    ),
                    textarea(`class` := "form-control", id := uid, onInput(onChange))("")
                )
            )
        )
    

    // private API
    // UI
    private def maybeRenderErrors() = 
        status
            .filter(s => s.kind == Page.StatusKind.ERROR && s.message.nonEmpty)
            .map(s => div(`class` := "form-errors")(s.message))
            .getOrElse(div())

    /* 
    check if the form is loaded(if it's present on the page)
    - document.getElementById("email") != null
    check again, while the element is null, with 

    use IO effects 
     */

     // logic
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

