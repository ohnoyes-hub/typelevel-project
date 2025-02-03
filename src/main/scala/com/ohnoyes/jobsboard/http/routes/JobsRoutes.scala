package com.ohnoyes.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.* // for Concurrent (Ref and Deferred)
import cats.implicits.*
import org.typelevel.log4cats.Logger
import tsec.authentication.asAuthed
import tsec.authentication.SecuredRequestHandler


import scala.collection.mutable
import scala.language.implicitConversions
import java.util.UUID
import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.domain.job.*
import com.ohnoyes.jobsboard.domain.pagination.*
import com.ohnoyes.jobsboard.domain.security.*
import com.ohnoyes.jobsboard.http.responses.*
import com.ohnoyes.jobsboard.http.validation.syntax.*
import org.checkerframework.checker.units.qual.s
import com.ohnoyes.jobsboard.logging.syntax.*


class JobsRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F], authenticator: Authenticator[F]) extends HttpValidationDsl[F] {

    private val securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)

    object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
    object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")


    // POST /jobs?limit=x&offset=y { filter } ( pagination)
    // TODO: add query params and filters.
    private val allJobsRoutes: HttpRoutes[F] = HttpRoutes.of[F] { 
        case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset)   => 
            for {
                filter <- req.as[JobFilter]
                jobsList <- jobs.all(filter, Pagination(limit, offset))
                resp <- Ok(jobsList)
            } yield resp
    }

    // GET /jobs/uuid
    private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar (id) => 
        jobs.find(id).flatMap {
            case Some(job) => Ok(job)
            case None => NotFound(FailureResponse(s"Job $id not found"))
        }
    }
    
    // POST /jobs/create { jobInfo}
    private val createJobRoute: AuthRoute[F] = { 
        case req @ POST -> Root / "create" asAuthed _ => 
            req.request.validate[JobInfo] { jobInfo =>
                for {
                    jobId <- jobs.create("TODO@ohnoyes.xyz", jobInfo)
                    resp <- Created(jobId)
                } yield resp
            }
    }

    // PUT /jobs/uuid { jobInfo }
    private val updateJobRoute: AuthRoute[F] = { 
        case req @ PUT -> Root / UUIDVar(id) asAuthed user => 
            req.request.validate[JobInfo] { jobInfo =>
                jobs.find(id).flatMap {
                    case None => 
                        NotFound(FailureResponse(s"Oh no, job $id not found. Cannot update"))
                    case Some(job) if user.owns(job) || user.isAdmin =>
                        jobs.update(id, jobInfo) *> Ok()
                    case _ => 
                        Forbidden(FailureResponse("Oh no! You can only update your own jobs"))
                }
            }
    }

    // DELETE /jobs/uuid
    private val deleteJobRoute: AuthRoute[F] = { 
        case req @ DELETE -> Root / UUIDVar(id) asAuthed user => 
            jobs.find(id).flatMap {
                case None => NotFound(FailureResponse(s"Oh noes, job $id not found. No delete"))
                case Some(job) if user.owns(job) || user.isAdmin => 
                    jobs.delete(id) *> Ok()
                case _ => Forbidden(FailureResponse("Oh no! You can only delete your own jobs"))
            }
    }

    val unauthRoutes = allJobsRoutes <+> findJobRoute
    val authRoutes = securedHandler.liftService(
        createJobRoute.restrictedTo(allRoles) |+|
        updateJobRoute.restrictedTo(allRoles) |+|
        deleteJobRoute.restrictedTo(allRoles)
    )
    val routes = Router(
        "/jobs" -> (unauthRoutes <+> authRoutes)
    )
}

object JobsRoutes {
    def apply[F[_]: Concurrent: Logger](jobs: Jobs[F], authenticator: Authenticator[F]) = new JobsRoutes[F](jobs, authenticator)
}
