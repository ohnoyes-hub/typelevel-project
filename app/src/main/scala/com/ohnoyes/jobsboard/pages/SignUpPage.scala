package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.http.*
import tyrian.Html.*
import cats.effect.IO
import tyrian.cmds.Logger
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*

import com.ohnoyes.jobsboard.common.*
import com.ohnoyes.jobsboard.domain.auth.*

import com.ohnoyes.jobsboard.*

// form
/* 
    NewUserInfo input:
        - email
        - password
        - confirm password
        - first name
        - last name
        - company
    button - trigger sign up
 */
final case class SignUpPage(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    firstName: String = "",    
    lastName: String = "",
    company: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Sign Up", status) {
    import SignUpPage.*

    // API
    

    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
        case UpdateEmail(e) => (this.copy(email=e), Cmd.None) //Logger.consoleLog[IO](s"Changing email: $email"))
        case UpdatePassword(p) => (this.copy(password=p), Cmd.None)
        case UpdateConfirmPassword(cp) => (this.copy(confirmPassword=cp), Cmd.None)
        case UpdateFirstName(fn) => (this.copy(firstName=fn), Cmd.None)
        case UpdateLastName(ln) => (this.copy(lastName=ln), Cmd.None)
        case UpdateCompany(c) => (this.copy(company=c), Cmd.None)
        case AttemptSignUp => 
            if (!email.matches(Constants.emailRegex))
                (setErrorStatus("Invalid email"), Cmd.None)
            else if (password.isEmpty)
                (setErrorStatus("Please enter a password"), Cmd.None)
            else if (password != confirmPassword)
                (setErrorStatus("Passwords do not match"), Cmd.None)
            else
                (this, Commands.signup(
                    NewUserInfo(
                        email, 
                        password, 
                        Option(firstName).filter(_.nonEmpty), 
                        Option(lastName).filter(_.nonEmpty), 
                        Option(company).filter(_.nonEmpty)
                        )
                    )
                )
        case SignUpError(message) => (setErrorStatus(message), Cmd.None)
        case SignUpSuccess(message) => (setSuccessStatus(message), Cmd.None)
        case _ => (this, Cmd.None)
    }

    override def renderFormContent(): List[Html[App.Msg]] = List(
        renderInput("Email", "email", "text", true, UpdateEmail(_)),
        renderInput("Password", "password", "password", true, UpdatePassword(_)),
        renderInput("Confirm Password", "confirmpassword", "password", true, UpdateConfirmPassword(_)),
        renderInput("First Name", "firstname", "text", false, UpdateFirstName(_)),
        renderInput("Last Name", "lastname", "text", false, UpdateLastName(_)),
        renderInput("Company", "company", "text", false, UpdateCompany(_)),
        // button
        button(`type` := "button", onClick(AttemptSignUp))("Sign Up")
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

object SignUpPage {
    trait Msg extends App.Msg
    case class UpdateEmail(email: String) extends Msg
    case class UpdatePassword(password: String) extends Msg
    case class UpdateConfirmPassword(confirmPassword: String) extends Msg
    case class UpdateFirstName(firstName: String) extends Msg
    case class UpdateLastName(lastName: String) extends Msg
    case class UpdateCompany(company: String) extends Msg
    // actions
    case object AttemptSignUp extends Msg
    case object NoOp extends Msg
    // statuses
    case class SignUpError(message: String) extends Msg
    case class SignUpSuccess(message: String) extends Msg

    object Endpoints {
        val signup = new Endpoint[Msg] {
            val location = Constants.endpoints.signup
            val method = Method.Post
            val onResponse: Response => Msg = response => response.status match {
                case Status(201, _) => 
                    SignUpSuccess("OH YES: You can now log in.")
                case Status(s, _) if s >= 400 && s < 500 => 
                    val json = response.body
                    val parsed = parse(json).flatMap(json => json.hcursor.get[String]("error"))
                    parsed match {
                        case Left(e) => SignUpError(s"Oh No Error: ${e.getMessage}")
                        case Right(error) => SignUpError(error)
                        
                    }
            }
            val onError: HttpError => Msg = 
                e => SignUpError(e.toString())
        }
    }

    object Commands {
        def signup(newUserInfo: NewUserInfo): Cmd[IO, Msg] = 
            Endpoints.signup.call(newUserInfo)
    }

}