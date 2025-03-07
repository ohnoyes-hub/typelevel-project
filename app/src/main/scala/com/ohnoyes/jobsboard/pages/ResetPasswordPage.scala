package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.http.*
import tyrian.Html.*
import cats.effect.IO
import io.circe.parser.*
import io.circe.generic.auto.*

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.common.*
import com.ohnoyes.jobsboard.domain.auth.*
import com.ohnoyes.jobsboard.components.*

// email, token, new password + button
final case class ResetPasswordPage(
    email: String = "",
    token: String = "",
    password: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Reset Password", status) {
    import ResetPasswordPage.*
    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
        case UpdateEmail(e) => 
            (this.copy(email = e), Cmd.None)
        case UpdateToken(token) => 
            (this.copy(token = token), Cmd.None)
        case UpdatePassword(p) =>
            (this.copy(password = p), Cmd.None)
        // action
        case AttemptResetPassword => 
            if(!email.matches(Constants.emailRegex))
                (setErrorStatus("Oh no, please enter valid email."), Cmd.None)
            else if(token.isEmpty)
                (setErrorStatus("Oh no, please enter token."), Cmd.None)
            else if(password.isEmpty)
                (setErrorStatus("Oh no, please enter password."), Cmd.None)
            else
                (this, Commands.resetPassword(email, token, password))
        case ResetPasswordFailure(error) => 
            (setErrorStatus(error), Cmd.None)
        case ResetPasswordSuccess =>
            (setSuccessStatus("Yes! Password reset success!"), Cmd.None)
        case _ => 
            (this, Cmd.None)
    }

    override protected def renderFormContent(): List[Html[App.Msg]] = List(
        renderInput("Email", "email", "text", true, UpdateEmail(_)),
        renderInput("Token", "token", "text", true, UpdateToken(_)),
        renderInput("Password", "password", "password", true, UpdatePassword(_)),
        button(`type` := "button", onClick(AttemptResetPassword))("Reset Password"),
        Anchors.renderSimpleNavLink("Oh, no token yet?", Page.Urls.FORGOT_PASSWORD)
    )

    //////////////////////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////////////////////
    
    // util - this.copy to keep case class intack(why we can't move it FormPage)
    def setErrorStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

    def setSuccessStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

} 
    
object ResetPasswordPage {
    trait Msg extends App.Msg
    case class UpdateEmail(email: String) extends Msg
    case class UpdateToken(token: String) extends Msg
    case class UpdatePassword(password: String) extends Msg
    case object AttemptResetPassword extends Msg
    case class ResetPasswordFailure(error: String) extends Msg
    case object ResetPasswordSuccess extends Msg

    object Endpoints {
        val resetPassword = new Endpoint[Msg] {
            override val location: String = Constants.endpoints.resetPassword
            override val method: Method = Method.Post
            override val onError: HttpError => Msg = e => ResetPasswordFailure(e.toString())
            override val onResponse: Response => Msg = response => response.status match {
                case Status(200, _) => ResetPasswordSuccess
                case Status(s, _) if s >= 400 && s < 500 => 
                    val json = response.body
                    val parsed = parse(json).flatMap(json => json.hcursor.get[String]("error"))
                    parsed match {
                        case Left(e) => ResetPasswordFailure(s"Response error: ${e.getMessage}")
                        case Right(serverError) => ResetPasswordFailure(serverError)   
                    }
                case _ => ResetPasswordFailure("Oh no! Unknown error.")
            }
        }
    }

    object Commands {
        def resetPassword(email: String, token: String, password: String): Cmd[IO, Msg] = 
            Endpoints.resetPassword.call(RecoverPasswordInfo(email, token, password))
    }
}
