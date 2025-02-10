package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.Html.*
import tyrian.http.* 
import cats.effect.IO
import io.circe.generic.auto.*
import tyrian.cmds.Logger

import com.ohnoyes.jobsboard.common.*
import com.ohnoyes.jobsboard.domain.auth.* 


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
) extends Page {
    import LoginPage.*
    // API
    override def initCmd: Cmd[IO, Page.Msg] = 
        Cmd.None
    override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match {
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
            (setSuccessStatus("Yes! Login successful"), Logger.consoleLog[IO](s"You Got Token: $token"))
        case _ => (this, Cmd.None)
    }
        
    override def view(): Html[Page.Msg] = 
        div( `class` := "form-selection")(
            // Ttile: Sign Up
            div(`class` := "top-section")(
                h1("Sign Up")
            ),
            // form
            form(name := "signin", `class` := "form", onEvent("submit",
                e => {
                    e.preventDefault()
                    NoOp
                }))(
                    renderInput("Email", "email", "email", true, UpdateEmail(_)),
                    renderInput("Password", "password", "password", true, UpdatePassword(_)),
                    button(`type` := "button", onClick(AttempLogin))("Login")

                ),
                    status.map(s => div(s.message)).getOrElse(div())
            )

    //////////////////////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////////////////////

    // UI
    private def renderInput(
        name: String, 
        uid: String, 
        kind: String, 
        isRequired: Boolean, 
        onChange: String => Msg
    ) = 
        div(`class` := "form=input")(
            label(`for` := name, `class` := "form-label")(
                if (isRequired) span("*") else span(),
                text(name)
            ),
            input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
        )
    
    // util
    def setErrorStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

    def setSuccessStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
} 

object LoginPage {
    // Msg
    trait Msg extends Page.Msg
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
            override val location: String = Constants.Endpoints.login
            override val method: Method = Method.Post
            override val onError: HttpError => Msg = 
                e => LoginError(e.toString)
            override val onSuccess: Response => Msg = response => {
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