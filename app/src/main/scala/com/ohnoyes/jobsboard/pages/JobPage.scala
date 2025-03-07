package com.ohnoyes.jobsboard.pages

import tyrian.* 
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO
import io.circe.generic.auto.*

// Laika - for markdown rendering
import laika.api.*
import laika.format.*

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.common.*
import com.ohnoyes.jobsboard.domain.job.*

final case class JobPage(
    id: String,
    maybeJob: Option[Job] = None,
    status: Page.Status = Page.Status.LOADING
) extends Page {
    import JobPage.*
    // API
    override def initCmd: Cmd[IO, App.Msg] = 
        Commands.getJob(id)

    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
        case SetError(e) => (setErrorStatus(e), Cmd.None)
        case SetJob(job) => 
            (setSuccessStatus("Yes, Job loaded").copy(maybeJob = Some(job)), Cmd.None)
        case _ => (this, Cmd.None)
    }
    override def view(): Html[App.Msg] = maybeJob match
        case Some(job) => renderJobPage(job)
        case None => renderNoJobPage()
    
        

    //////////////////////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////////////////////

    // UI
    private def renderJobPage(job: Job) =
        div(`class` := "job-page")(
            div(`class` := "job-hero")(
                img(
                    `class` := "job-logo",
                    src := job.jobInfo.image.getOrElse(""),
                    alt := job.jobInfo.company
                ),
                h1(s"${job.jobInfo.company} - ${job.jobInfo.title}")
            ),
            div(`class` := "job-overview")(
                renderJobDetails(job)
            ),
            renderJobDescription(job),
            a(href := job.jobInfo.externalUrl, `class` := "job-apply-action", target := "blank")("Apply")
        )
    
    private def renderJobDetails(job: Job) = {
        def renderDetail(value: String) =
            if(value.isEmpty) div()
            else li(`class` := "job-detail-item")(value)
            
        val fullLocationString = job.jobInfo.country match {
            case Some(country) => s"${job.jobInfo.location}, $country"
            case None => job.jobInfo.location
        }

        val currency = job.jobInfo.currency.getOrElse("")

        val fullSalaryString = (job.jobInfo.salaryLow, job.jobInfo.salaryHi) match {
            case (Some(low), Some(high)) => s"$currency $low - $high"
            case (Some(low), None) => s"From $currency $low"
            case (None, Some(high)) => s"Up to $currency $high"
            case _ => "Not specified"
        }

        div(`class` := "job-details")(
            ul(`class` := "job-detail")(
                renderDetail(fullLocationString),
                renderDetail(fullSalaryString),
                renderDetail(job.jobInfo.seniority.getOrElse("No level specified")),
                renderDetail(job.jobInfo.tags.getOrElse(List()).mkString(", "))
            )
        )
    }
                
    private def renderJobDescription(job: Job) = {
        val descriptionHtml = markdownTransformer.transform(job.jobInfo.description) match {
            case Left(value) => 
                """
                OH NO! Something went wrong while rendering the job description markdown.
                Apply button is still there, so you can still apply for the job.
                """
            case Right(html) => html
        }
        div(`class` := "job-description")().innerHtml(descriptionHtml)
    }
        

    private def renderNoJobPage() = status.kind match {
        case Page.StatusKind.LOADING => div("Loading...")
        case Page.StatusKind.ERROR => 
            div("Oh no! Page either doesn't exist or something went wrong")
        case Page.StatusKind.SUCCESS =>
            div("Hmmm, you reached this page despite server being healthy but no job...")
    }
        

    // logic
    val markdownTransformer = Transformer
        .from(Markdown)
        .to(HTML)
        .build

    def setErrorStatus(message: String) =
        this.copy(status = Page.Status(message, Page.StatusKind.ERROR))

    def setSuccessStatus(message: String) =
        this.copy(status = Page.Status(message, Page.StatusKind.SUCCESS))
} 

object JobPage {
    trait Msg extends App.Msg
    case class SetError(msg: String) extends Msg
    case class SetJob(job: Job) extends Msg

    object Endpoints {
        def getJob(id: String) = new Endpoint[Msg] {
            override val location: String = Constants.endpoints.jobs + s"/$id"
            override val method: Method = Method.Get
            override val onError: HttpError => Msg = e => SetError(e.toString)
            override val onResponse: Response => Msg = 
                Endpoint.onResponse[Job, Msg](SetJob(_), SetError(_))
        }
    }

    object Commands {
        def getJob(id: String) = Endpoints.getJob(id).call()
    }
}
