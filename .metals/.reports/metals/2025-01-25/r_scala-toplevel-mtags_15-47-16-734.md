error id: file://<WORKSPACE>/src/main/scala/com/ohnoyes/jobsboard/http/routes/HealthRoutes.scala:[419..419) in Input.VirtualFile("file://<WORKSPACE>/src/main/scala/com/ohnoyes/jobsboard/http/routes/HealthRoutes.scala", "package com.ohnoyes.jobsboard.http.routes

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.Monad

class HealthRoutes[F[_]: Monad] extends Http4sDsl[F] {
    val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case GET -> Root / "health" => Ok("I am healthy!")
    }

    val routes = Router(
        "/health" -> healthRoute
    )
    
}

object 
")
file://<WORKSPACE>/file:<WORKSPACE>/src/main/scala/com/ohnoyes/jobsboard/http/routes/HealthRoutes.scala
file://<WORKSPACE>/src/main/scala/com/ohnoyes/jobsboard/http/routes/HealthRoutes.scala:21: error: expected identifier; obtained eof

^
#### Short summary: 

expected identifier; obtained eof