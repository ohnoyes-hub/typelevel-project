package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.Html.*
import cats.effect.IO
import tyrian.http.*
import io.circe.generic.auto.*

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.common.*
import com.ohnoyes.jobsboard.components.*
import com.ohnoyes.jobsboard.domain.auth.*

final case class ForgotPasswordPage(email: String = "", status: Option[Page.Status] = None) extends FormPage("Reset Password", status) {
    import ForgotPasswordPage.*

    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
        case UpdateEmail(e) =>
            (this.copy(email = e), Cmd.None)
        case AttemptResetPassword => 
            if(!email.matches(Constants.emailRegex))
                (setErrorStatus("Oh no, your email is invalid. Try again?"), Cmd.None)
            else
                (this, Commands.resetPassword(email))
        case ResetSuccess => 
            (setSuccessStatus("Sent! Please check your email"), Cmd.None)
        case ResetFailure(error) => 
            (setErrorStatus(error), Cmd.None)
        case _ =>
            (this, Cmd.None)
    }

    override protected def renderFormContent(): List[Html[App.Msg]] = List(
        renderInput("Email", "email", "text", true, UpdateEmail(_)),
        button(`type` := "button", onClick(AttemptResetPassword))("Send Reset Token"),
        Anchors.renderSimpleNavLink("Have a reset token?", Page.Urls.RESET_PASSWORD, "auth-link")
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

object ForgotPasswordPage {
    trait Msg extends App.Msg
    case class UpdateEmail(email: String) extends Msg
    case object AttemptResetPassword extends Msg
    case class ResetFailure(error: String) extends Msg
    case object ResetSuccess extends Msg 

    object Endpoints {
        val resetPassword = new Endpoint[Msg] {
            override val location: String = Constants.endpoints.forgotPassword
            override val method: Method = Method.Post
            override val onError: HttpError => Msg = e => ResetFailure(e.toString())
            override val onResponse: Response => Msg = _ => ResetSuccess

        }
    }

    object Commands {
        def resetPassword(email: String) =
            Endpoints.resetPassword.call(ForgotPasswordInfo(email))
    }
}

