package com.ohnoyes.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO
import io.circe.generic.auto.*

import com.ohnoyes.jobsboard.* 
import com.ohnoyes.jobsboard.core.* 
import com.ohnoyes.jobsboard.common.* 
import com.ohnoyes.jobsboard.domain.auth.*

final case class ProfilePage(
    oldPassword: String = "",
    newPassword: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Profile", status) {
    import ProfilePage.*

    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
        case UpdateOldPassword(p) => 
            (this.copy(oldPassword = p), Cmd.None)
        case UpdateNewPassword(nP) => 
            (this.copy(newPassword = nP), Cmd.None)
        case AttemptChangePassword =>
            (this, Commands.changePassword(oldPassword, newPassword))
        case ChangePasswordFailure(error) =>
            (setErrorStatus(error), Cmd.None)
        case ChangePasswordSuccess =>
            (setSuccessStatus("Yes! Password change success!"), Cmd.None)
        case _ => (this, Cmd.None)
    }

    override def view(): Html[App.Msg] = // hides profile page if not logged in
        if (Session.isActive) super.view()
        else renderInvalidPage
    
    override protected def renderFormContent(): List[Html[App.Msg]] = List(
        renderInput("Old Password", "oldPassword", "password", true, UpdateOldPassword(_)),
        renderInput("New Password", "newPassword", "password", true, UpdateNewPassword(_)),
        button(`type` := "button", onClick(AttemptChangePassword))("Change Password")
    )

    //////////////////////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////////////////////
    
    // util - this.copy to keep case class intack(why we can't move it FormPage)
    def setErrorStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

    def setSuccessStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

    private def renderInvalidPage = 
        div(
            h1("Profile"),
            div("Oh no! You need to login to view this page.")
        )
}


object ProfilePage {
    trait Msg extends App.Msg

    case class UpdateOldPassword(oldPassword: String) extends Msg
    case class UpdateNewPassword(newPassword: String) extends Msg
    case object AttemptChangePassword extends Msg
    case class ChangePasswordFailure(error: String) extends Msg
    case object ChangePasswordSuccess extends Msg

    object Endpoints {
        val changePassword = new Endpoint[Msg] {
            override val location: String = Constants.endpoints.changePassword
            override val method: Method = Method.Put
            override val onError: HttpError => Msg = e => ChangePasswordFailure(e.toString())
            override val onResponse: Response => Msg = _.status match
                case Status(200, _) => ChangePasswordSuccess
                case Status(404, _) => ChangePasswordFailure("Oh no! User not found. How did you get here?")
                case Status(s, _) if s >= 400 && s < 500 => ChangePasswordFailure(s"Invalid credential status: $s")
                case _ => ChangePasswordFailure("Oh no! Unknown server error.")
        }
    }

    object Commands {
        def changePassword(oldPassword: String, newPassword: String) =
            Endpoints.changePassword.callAuthorized(NewPasswordInfo(oldPassword, newPassword))
    }
}

