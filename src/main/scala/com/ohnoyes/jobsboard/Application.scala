package com.ohnoyes.jobsboard

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.*
import cats.effect.IO
import cats.*
import cats.implicits.*
import org.http4s.ember.server.EmberServerBuilder

import com.ohnoyes.jobsboard.http.routes.HealthRoutes
import pureconfig.ConfigSource
import com.ohnoyes.jobsboard.config.*
import com.ohnoyes.jobsboard.config.syntax.*
import com.typesafe.config.Config
import pureconfig.error.ConfigReaderException


/* 
TODO: 
    1- create the health endpoint to our app
    2- add minimal configuration
    3- basic
 */
object Application extends IOApp.Simple {

    val configSource = ConfigSource.default.load[EmberConfig]

    override def run: IO[Unit] = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config => 
        EmberServerBuilder
            .default[IO]
            .withHost(config.host) 
            .withPort(config.port)
            .withHttpApp(HealthRoutes[IO].routes.orNotFound)
            .build
            .use(_ => IO("Server ready, OH NOES YES!") *> IO.never)
        }
        
}
