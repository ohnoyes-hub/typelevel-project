package com.ohnoyes.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO
import cats.syntax.traverse.*
import io.circe.generic.auto.*
import io.circe.parser.* 
import tyrian.cmds.Logger
import org.scalajs.dom.File
import org.scalajs.dom.FileReader

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.common.*
import com.ohnoyes.jobsboard.domain.job.*
import scala.util.Try

case class PostJobPage(
    company: String = "",
    title: String = "",
    description: String = "",
    externalUrl: String = "",
    remote: Boolean = false,
    location: String = "",
    salaryLow: Option[Int] = None,
    salaryHi: Option[Int] = None,
    currency: Option[String] = None,
    country: Option[String] = None,
    tags: Option[String] = None, // TODO parse the tags before sending to the server
    image: Option[String] = None,
    seniority: Option[String] = None,
    other: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post Job", status) {
    import PostJobPage.*

    override def view(): Html[App.Msg] = 
        if (Session.isActive) super.view()
        else renderInvalidPage

    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
        case UpdateCompany(v) => 
            (this.copy(company = v), Cmd.None)
        case UpdateTitle(v) => 
            (this.copy(title = v), Cmd.None)
        case UpdateDescription(description) => 
            (this.copy(description = description), Cmd.None)
        case UpdateExternalUrl(v)  =>
            (this.copy(externalUrl = v), Cmd.None)
        case ToggleRemote =>
            (this.copy(remote = !this.remote), Cmd.None)
        case UpdateLocation(v) => 
            (this.copy(location = v), Cmd.None)
        case UpdateSalaryLow(v) =>  
            (this.copy(salaryLow = Some(v)), Cmd.None)
        case UpdateSalaryHi(v) =>
            (this.copy(salaryHi = Some(v)), Cmd.None)
        case UpdateCurrency(v) =>
            (this.copy(currency = Some(v)), Cmd.None)
        case UpdateCountry(v) =>
            (this.copy(country = Some(v)), Cmd.None)
        case UpdateTags(v) => 
            (this.copy(tags = Some(v)), Cmd.None)
        case UpdateImageFile(maybeFile) =>
            (this, Commands.loadFile(maybeFile))
        case UpdateImage(maybeImage) =>
            (this.copy(image = maybeImage), Logger.consoleLog[IO]("Image yoinked: " + maybeImage))
        case UpdateSeniority(v) =>
            (this.copy(seniority = Some(v)), Cmd.None)
        case UpdateOther(v) =>
            (this.copy(other = Some(v)), Cmd.None)
        case AttemptPostJob => 
            (this, Commands.postJob(
                company,
                title,
                description,
                externalUrl,
                remote,
                location,
                salaryLow,
                salaryHi,
                currency,
                country,
                tags,
                image,
                seniority,
                other
            ))
        case PostJobError(error) => 
            (setErrorStatus(error), Cmd.None)
        case PostJobSuccess(jobId) =>
            (setSuccessStatus(s"Yes! Job $jobId posted successfully!"), Logger.consoleLog[IO](s"Posted job with $jobId"))
        case _ => (this, Cmd.None)
    }
    
    override protected def renderFormContent(): List[Html[App.Msg]] = List(
        renderInput("Company", "company", "text", true, UpdateCompany(_)),
        renderInput("Title", "title", "text", true, UpdateTitle(_)),
        renderTextArea("Description", "description", true, UpdateDescription(_)),
        renderInput("ExternalUrl", "externalUrl", "text", true, UpdateExternalUrl(_)),
        renderInput("Remote", "remote", "checkbox", false, _ => ToggleRemote),
        renderInput("Location", "location", "text", true, UpdateLocation(_)),
        renderInput("SalaryLow", "salaryLow", "number", false, s => UpdateSalaryLow(parseNumber(s))),
        renderInput("SalaryHi", "salaryHi", "number", false, s => UpdateSalaryHi(parseNumber(s))),
        renderInput("Currency", "currency", "text", false, UpdateCurrency(_)),
        renderInput("Country", "country", "text", false, UpdateCountry(_)),
        renderInput("Tags", "tags", "text", false, UpdateTags(_)),
        renderImageUploadInput("Logo", "logo", image, UpdateImageFile(_)),
        renderInput("Seniority", "seniority", "text", false, UpdateSeniority(_)),
        renderInput("Other", "other", "text", false, UpdateOther(_)), 
        button(`type` := "button", onClick(AttemptPostJob))("Post Job")
    )

    //////////////////////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////////////////////
    
    // util
    def setErrorStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

    def setSuccessStatus(message: String): Page =
        this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

    private def parseNumber(s: String) = 
        Try(s.toInt).getOrElse(0)
    
    // UI
    private def renderInvalidPage = 
        div(
            h1("Post Job"),
            div("You need to be logged in to post a job"),
        )

}

object PostJobPage {
    trait Msg extends App.Msg
    case class UpdateCompany(company: String) extends Msg
    case class UpdateTitle(title: String) extends Msg
    case class UpdateDescription(description: String) extends Msg
    case class UpdateExternalUrl(externalUrl: String) extends Msg
    case class UpdateLocation(location: String) extends Msg
    case object ToggleRemote extends Msg
    case class UpdateSalaryLow(salaryLow: Int) extends Msg
    case class UpdateSalaryHi(salaryHi: Int) extends Msg
    case class UpdateCurrency(currency: String) extends Msg
    case class UpdateCountry(country: String) extends Msg
    case class UpdateTags(tags: String) extends Msg
    case class UpdateImageFile(maybeFile: Option[File]) extends Msg
    case class UpdateImage(maybeImage: Option[String]) extends Msg
    case class UpdateSeniority(seniority: String) extends Msg
    case class UpdateOther(other: String) extends Msg
    case object AttemptPostJob extends Msg
    case class PostJobSuccess(jobId: String) extends Msg
    case class PostJobError(error: String) extends Msg

    object Endpoint {
        val postJob = new Endpoint[Msg] {
            override val location: String = Constants.endpoints.postJob
            override val method: Method = Method.Post
            override val onError: HttpError => Msg = e => PostJobError(e.toString())
            override val onResponse: Response => Msg = response => response.status match {
                case Status(s, _) if s >= 200 && s < 300 =>
                    val jobId = response.body
                    PostJobSuccess(jobId)
                case Status(401, _) =>
                    PostJobError("Oh no, you need to be logged in to post a job")
                case Status(s, _) if s >= 400 && s < 500 =>
                    val json = response.body
                    val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
                    parsed match {
                        case Left(e) => PostJobError(s"Oh no: $e")
                        case Right(error) => PostJobError(error)
                    }
                case _ => 
                    PostJobError("Oh no, something went wrong from the server")
            }
        }
    }

    object Commands {
        def postJob(
            company: String,
            title: String,
            description: String,
            externalUrl: String,
            remote: Boolean,
            location: String,
            salaryLow: Option[Int],
            salaryHi: Option[Int],
            currency: Option[String],
            country: Option[String],
            tags: Option[String],
            image: Option[String],
            seniority: Option[String],
            other: Option[String]
        ) = 
            Endpoint.postJob.callAuthorized(
                JobInfo(
                    company,
                    title,
                    description,
                    externalUrl,
                    remote,
                    location,
                    salaryLow,
                    salaryHi,
                    currency,
                    country,
                    tags.map(text => text.split(",").map(_.trim).toList),
                    image,
                    seniority,
                    other
                )
            )

        def loadFile(maybeFile: Option[File]) =
            Cmd.Run[IO, Option[String], Msg](
                // run effect that retusn a Option[String]
                // Option[File] => Option[String]
                // traverse: Option[File].traverse(file => IO[String]) => IO[Option[String]]
                maybeFile.traverse { file =>
                    IO.async_ { cb => 
                        // create a reader
                        val reader = new FileReader
                        // set the onload
                        reader.onload = _ => cb(Right(reader.result.toString))
                        // trigger the reader
                        reader.readAsDataURL(file)
                    }
                }
            )(UpdateImage(_))
    }
}