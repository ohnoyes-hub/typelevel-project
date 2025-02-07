package com.ohnoyes.jobsboard.components

import tyrian.*
import tyrian.Html.*
import com.ohnoyes.jobsboard.core.* 

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Header {

    // public API
    def view() = 
        div(`class` := "header-container")(
            renderLogo(),
            div(`class` := "header-nav")(
                ul(`class`:= "header-links")(
                    renderNavLink("Jobs", "/jobs"),
                    renderNavLink("Login", "/login"),
                    renderNavLink("Signup", "/signup")
                    )
                )
                
        )

    // private API
    @js.native
    @JSImport("/static/img/logo.png", JSImport.Default)
    private val logoImage: String = js.native

    private def renderLogo() = 
        a(
            href := "/", 
            onEvent(
                "click", 
                e => {
                    e.preventDefault() 
                    Router.ChangeLocation("/")
                }
            )
        )(
            img(
                `class` := "home-logo",
                src := logoImage,
                alt := "OhNoYes"
            )
        )

    private def renderNavLink(text: String, location: String) = 
        li(`class` := "nav-item")(
            a(
                href := location, 
                `class` := "nav-link", 
                onEvent(
                    "click", 
                    e => {
                        e.preventDefault() 
                        Router.ChangeLocation(location)
                    }
                )
            )(text)
        )
}
