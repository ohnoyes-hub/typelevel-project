package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.Html.*
import tyrian.http.* 
import cats.effect.IO
import io.circe.generic.auto.*
import tyrian.cmds.Logger

import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.common.*
import com.ohnoyes.jobsboard.domain.auth.* 
import com.ohnoyes.jobsboard.core.Session

import com.ohnoyes.jobsboard.*

/* 
form
    - email
    - password
    - button - trigger login
status (success or failure)
 */
final case class LoginPage(
    email: String = "",
    password: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Login", status) {
    import LoginPage.*
    // API
    
    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
        case UpdateEmail(e) => (this.copy(email=e), Cmd.None) //Logger.consoleLog[IO](s"Changing email: $email"))
        case UpdatePassword(p) => (this.copy(password=p), Cmd.None)
        case AttempLogin => 
            if (!email.matches(Constants.emailRegex))
                (setErrorStatus("Oh no, invalid email"), Cmd.None)
            else if (password.isEmpty)
                (setErrorStatus("Please enter a password"), Cmd.None)
            else
                (this, Commands.login(LoginInfo(email, password)))
        case LoginError(error) => 
            (setErrorStatus(error), Cmd.None)
        case LoginSuccess(token) =>
            (setSuccessStatus("Yes! Login successful"), Cmd.Emit(Session.SetToken(email, token, isNewUser = true))) // Session.SetToken(email, token) should be a App.Msg
        case _ => (this, Cmd.None)
    }
        
    override def renderFormContent(): List[Html[App.Msg]] = List(
        renderInput("Email", "email", "email", true, UpdateEmail(_)),
        renderInput("Password", "password", "password", true, UpdatePassword(_)),
        button(`type` := "button", onClick(AttempLogin))("Login"),
        renderAuxLink(Page.Urls.FORGOT_PASSWORD, "Forgot Password?")
    )

    //////////////////////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////////////////////
    
    // util
    def setErrorStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

    def setSuccessStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
} 

object LoginPage {
    // Msg
    trait Msg extends App.Msg
    case class UpdateEmail(email: String) extends Msg
    case class UpdatePassword(password: String) extends Msg
    // actions
    case object AttempLogin extends Msg
    case object NoOp extends Msg
    // results
    case class LoginError(error: String) extends Msg
    case class LoginSuccess(token: String) extends Msg

    object Endpoints {
        val login = new Endpoint[Msg]{
            override val location: String = Constants.endpoints.login
            override val method: Method = Method.Post
            override val onError: HttpError => Msg = 
                e => LoginError(e.toString)
            override val onResponse: Response => Msg = response => {
                val maybeToken = response.headers.get("authorization")
                maybeToken match {
                    case Some(token) => LoginSuccess(token)
                    case None => LoginError("Oh no! Invalid username or password.")
                }
            }

        }
    }

    object Commands {
        def login(loginInfo: LoginInfo) =
            Endpoints.login.call(loginInfo)
    }
}