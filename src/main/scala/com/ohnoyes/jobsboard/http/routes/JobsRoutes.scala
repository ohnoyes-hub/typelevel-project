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
import com.ohnoyes.jobsboard.logging.syntax.*

import scala.collection.mutable
import java.util.UUID
import com.ohnoyes.jobsboard.domain.job.*
import com.ohnoyes.jobsboard.domain.pagination.*
import com.ohnoyes.jobsboard.http.responses.*
import com.ohnoyes.jobsboard.http.validation.syntax.*
import org.checkerframework.checker.units.qual.s
import com.ohnoyes.jobsboard.core.*


class JobsRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends HttpValidationDsl[F] {

    object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
    object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")


    // POST /jobs?limit=x&offset=y { filter } ( pagination)
    // TODO: add query params and filters.
    private val allJobsRoutes: HttpRoutes[F] = HttpRoutes.of[F] { 
        case req@  POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset)   => 
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
    private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { 
        case req @ POST -> Root / "create" => 
            req.validate[JobInfo] { jobInfo =>
                for {
                    jobId <- jobs.create("TODO@ohnoyes.xyz", jobInfo)
                    resp <- Created(jobId)
                } yield resp
            }
    }

    // PUT /jobs/uuid { jobInfo }
    private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { 
        case req @ PUT -> Root / UUIDVar(id) => 
            req.validate[JobInfo] { jobInfo =>
                for {
                    maybeNewJob <- jobs.update(id, jobInfo)
                    resp <- maybeNewJob match {
                        case Some(job) => Ok()
                        case None => NotFound(FailureResponse(s"Oh no, job $id not found. Cannot update"))
                    }
                } yield resp
            }
    }

    // DELETE /jobs/uuid
    private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { 
        case req @ DELETE -> Root / UUIDVar(id) => 
            jobs.find(id).flatMap {
                case Some(job) => 
                    for {
                        _ <- jobs.delete(id)
                        resp <- Ok()
                    } yield resp
                case None => NotFound(FailureResponse(s"Oh no, job $id not found. Cannot delete"))
            }
    }


    val routes = Router(
        "/jobs" -> (allJobsRoutes <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
    )
}

object JobsRoutes {
    def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobsRoutes[F](jobs)
}
