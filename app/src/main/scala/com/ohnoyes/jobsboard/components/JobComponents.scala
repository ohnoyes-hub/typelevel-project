package com.ohnoyes.jobsboard.components

import tyrian.*
import tyrian.Html.*

import com.ohnoyes.jobsboard.*
import com.ohnoyes.jobsboard.pages.Page
import com.ohnoyes.jobsboard.domain.job.*
import com.ohnoyes.jobsboard.common.*


object JobComponents {

    def card(job: Job) =
        div(`class` := "jvm-recent-jobs-cards")(
            div(`class`:= "jvm-recent-jobs-card-img")(
                renderJobImage(job)
            ),
            div(`class` := "jvm-recent-jobs-card-contents")(
                h5(
                    Anchors.renderSimpleNavLink(
                        s"${job.jobInfo.company} - ${job.jobInfo.title}",
                        Page.Urls.JOB(job.id.toString()),
                        "job-title-link"
                    )
                ),
                renderJobSummary(job)
            ),
            div(`class` := "jvm-recent-jobs-card-btn-apply")(
                a(
                    href := job.jobInfo.externalUrl,
                    target := "blank" // open in new tab
                )(
                    button(`type`:="button",`class`:="btn btn-danger")("Apply")

                )
            )
        )

    def renderJobSummary(job: Job): Html[App.Msg] =
        div(`class` := "job-summary")(
            // salary
            renderDetail("dollar", fullSalaryString(job)),
            // location
            renderDetail("location-dot", fullLocationString(job)),
            // seniority
            maybeRenderDetail("ranking-star", job.jobInfo.seniority),
            // tags
            maybeRenderDetail("tags", job.jobInfo.tags.map(_.mkString(", ")))
        )
    
    def maybeRenderDetail(icon: String, maybeValue: Option[String]): Html[App.Msg] =
        maybeValue.map(value => renderDetail(icon, value)).getOrElse(div())

    def renderDetail(icon: String, value: String): Html[App.Msg] =
        div(`class` := "job-detail")(
            i(`class` := s"fa fa-$icon job-detail-icon")(),
            p(`class` := "job-detail-value")(value)
        )
    
    def renderJobImage(job: Job) =
        img(
            `class` := "img-fluid",
            src := job.jobInfo.image.getOrElse(Constants.jobImagePlaceholder),
            alt := job.jobInfo.title
        )

    // private
    private def fullSalaryString(job: Job) = {
        val currency = job.jobInfo.currency.getOrElse("")
        (job.jobInfo.salaryLow, job.jobInfo.salaryHi) match {
            case (Some(low), Some(high)) => s"$currency $low - $high"
            case (Some(low), None) => s"From $currency $low"
            case (None, Some(high)) => s"Up to $currency $high"
            case _ => "unspecified=∞"
        }
    }

    private def fullLocationString(job: Job) = job.jobInfo.country match {
            case Some(country) => s"${job.jobInfo.location}, $country"
            case None => job.jobInfo.location
        }
}
