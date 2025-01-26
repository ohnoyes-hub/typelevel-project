package com.ohnoyes.jobsboard.http.routes

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.*
import cats.implicits.*


class JobsRoutes[F[_]: Monad] private extends Http4sDsl[F] {

    // POST /jobs?offset=x&limit=y { filter } (with pagination: ?offset=x&limit=y)
    // TODO: add query params and filters.
    private val allJobsRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root => 
        Ok("TODO: All jobs")
    }

    // GET /jobs/uuid
    private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar (id) => 
        Ok(s"TODO: Find job with uuid: $id")
    }
    
    // POST /jobs { jobInfo}
    private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root / "create" => 
        Ok("TODO: Create job")
    }

    // PUT /jobs/uuid { jobInfo }
    private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case PUT -> Root / UUIDVar(id) => 
        Ok(s"TODO: Update job with uuid: $id")
    }

    // DELETE /jobs/uuid
    private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / UUIDVar(id) => 
        Ok(s"TODO: Delete job with uuid: $id")
    }


    val routes = Router(
        "/jobs" -> (allJobsRoutes <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
    )
}

object JobsRoutes {
    def apply[F[_]: Monad] = new JobsRoutes[F]
}
