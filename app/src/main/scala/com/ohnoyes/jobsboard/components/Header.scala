package com.ohnoyes.jobsboard.components

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.core.* 
import com.ohnoyes.jobsboard.pages.*
import com.ohnoyes.jobsboard.components.*

object Header {

    // public API
    def view() = 
        div(`class` := "header-container")(
            renderLogo(),
            div(`class` := "header-nav")(
                ul(`class`:= "header-links")(
                    renderNavLinks()
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

    private def renderNavLinks(): List[Html[App.Msg]] = {
        val constantLinks = List(
            Anchors.renderSimpleNavLink("Jobs", Page.Urls.JOBS),
            Anchors.renderSimpleNavLink("Post Job", Page.Urls.POST_JOB)
        )
        
        val unauthedLinks = List(
            Anchors.renderSimpleNavLink("Login", Page.Urls.LOGIN),
            Anchors.renderSimpleNavLink("Signup", Page.Urls.SIGNUP)
        )

        val authedLinks = List(
            Anchors.renderSimpleNavLink("Profile", Page.Urls.PROFILE),
            Anchors.renderNavLink("Logout", Page.Urls.HASH)(_ => Session.Logout)
        )

        constantLinks ++ (
            if (Session.isActive) authedLinks 
            else unauthedLinks
        )
    }
}
