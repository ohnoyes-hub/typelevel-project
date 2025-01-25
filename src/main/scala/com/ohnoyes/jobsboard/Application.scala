package com.ohnoyes.jobsboard

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.*
import cats.effect.IO
import org.http4s.ember.server.EmberServerBuilder

import com.ohnoyes.jobsboard.http.routes.HealthRoutes
import pureconfig.ConfigSource
import com.ohnoyes.jobsboard.config.*
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

    override def run: IO[Unit] = 
        configSource match {
            case Left(errors) => IO.raiseError[Nothing](ConfigReaderException(errors))
            case Right(config) => 
                EmberServerBuilder
                    .default[IO]
                    .withHost(config.host) // String, but need a Host
                    .withPort(config.port) // String, but need a Port
                    .withHttpApp(HealthRoutes[IO].routes.orNotFound)
                    .build
                    .use(_ => IO("Server ready, OH NOES YES!") *> IO.never)
        }
        
}
