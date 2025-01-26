package com.ohnoyes.jobsboard.http

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.*
import cats.implicits.*

import com.ohnoyes.jobsboard.http.routes.*

class HttpApi[F[_]: Monad] private {
    private val healthRoutes = HealthRoutes[F].routes 
    private val jobsRoutes = JobsRoutes[F].routes

    val endpoints = Router(
        "/api" -> (healthRoutes <+> jobsRoutes)
    )
}

object HttpApi {
    def apply[F[_]: Monad] = new HttpApi[F]
}
