package com.ohnoyes.jobsboard.pages

import tyrian.*
import tyrian.Html.*

import cats.effect.IO

object Page {
    trait Msg

    enum StatusKind {
        case SUCCESS, ERROR, LOADING
    }
    case class Status(message: String, kind: StatusKind)

    object Urls {
        val LOGIN = "/login"
        val SIGNUP = "/signup"
        val FORGOT_PASSWORD = "/forgotpassword"
        val RECOVER_PASSWORD = "/recoverpassword"
        val EMPTY = "/"
        val HOME = "/"
        val JOBS = "/jobs"
    }

    import Urls.*
    def get(location: String): Page = location match {
        case `LOGIN` => LoginPage()
        case `SIGNUP` => SignUpPage()
        case `FORGOT_PASSWORD` => ForgotPasswordPage()
        case `RECOVER_PASSWORD` => RecoverPasswordPage()
        case `EMPTY` | `HOME` | `JOBS` => JobListPage()
        case s"/jobs/${id}" => JobPage(id)
        case _ => NotFoundPage()
    }
}

abstract class Page {
    // API

    // send a command upon instantiating
    def initCmd: Cmd[IO, Page.Msg] 
    // update
    def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg])
    // render
    def view(): Html[Page.Msg]
  
}

// login page
// signup page
// forgot password page
// recover password page
// jobs list page == home page
// individual job page
// not found page

