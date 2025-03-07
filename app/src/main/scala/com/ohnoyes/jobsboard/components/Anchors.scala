package com.ohnoyes.jobsboard.components

import tyrian.*
import tyrian.Html.*

import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.*

object Anchors {
    def renderSimpleNavLink(text: String, location: String) = 
        renderNavLink(text, location)(Router.ChangeLocation(_))


    def renderNavLink(text: String, location: String)(location2msg: String => App.Msg) = 
        li(`class` := "nav-item")(
            a(
                href := location, 
                `class` := "nav-link", 
                onEvent(
                    "click", 
                    e => {
                        e.preventDefault() // native JS - prevent reloading the page
                        location2msg(location)
                    }
                )
            )(text)
        )
}
