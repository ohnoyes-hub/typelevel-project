package com.ohnoyes.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.* // for Concurrent (Ref and Deferred)
import cats.implicits.*

import scala.collection.mutable
import java.util.UUID
import com.ohnoyes.jobsboard.domain.job.*
import com.ohnoyes.jobsboard.http.responses.*


class JobsRoutes[F[_]: Concurrent] private extends Http4sDsl[F] {

    // "database"
    private val database = mutable.Map[UUID, Job]()

    // POST /jobs?offset=x&limit=y { filter } (with pagination: ?offset=x&limit=y)
    // TODO: add query params and filters.
    private val allJobsRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root => 
        Ok(database.values)
    }

    // GET /jobs/uuid
    private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar (id) => 
        database.get(id) match {
            case Some(job) => Ok(job)
            case None => NotFound(FailureResponse(s"Job $id not found"))
        }
    }
    
    // POST /jobs { jobInfo}
    private def createJob(jobInfo: JobInfo): F[Job] = 
        Job(
            id = UUID.randomUUID(), 
            date = System.currentTimeMillis(),
            ownerEmail = "TODO@ohnoyes.io",
            jobInfo = jobInfo,
            active = true
        ).pure[F]


    private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { 
        case req @ POST -> Root / "create" => 
            for {
                jobInfo <- req.as[JobInfo] // magic
                job <- createJob(jobInfo)
                resp <- Created(job)
            } yield resp
    }

    // PUT /jobs/uuid { jobInfo }
    private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { 
        case req @ PUT -> Root / UUIDVar(id) => 
            database.get(id) match {
                case Some(job) => 
                    for {
                        jobInfo <- req.as[JobInfo]
                        _ = database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
                        resp <- Ok()
                    } yield resp
                case None => NotFound(FailureResponse(s"Oh no, job $id not found. Cannot update"))
            }
    }

    // DELETE /jobs/uuid
    private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { 
        case req @ DELETE -> Root / UUIDVar(id) => 
            database.get(id) match {
                case Some(job) => 
                    for {
                        jobInfo <- req.as[JobInfo]
                        _ = database.remove(id).pure[F]
                        resp <- Ok()
                    } yield resp
                case None => NotFound(FailureResponse(s"Oh no, job $id not found. Cannot update"))
            }
    }


    val routes = Router(
        "/jobs" -> (allJobsRoutes <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
    )
}

object JobsRoutes {
    def apply[F[_]: Concurrent] = new JobsRoutes[F]
}
