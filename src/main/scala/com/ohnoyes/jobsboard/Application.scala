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
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.ohnoyes.jobsboard.http.HttpApi
import pureconfig.ConfigSource
import com.ohnoyes.jobsboard.config.*
import com.ohnoyes.jobsboard.config.syntax.*
import com.typesafe.config.Config
import pureconfig.error.ConfigReaderException
import org.typelevel.log4cats.slf4j.Slf4jLogger


/* 
TODO: 
    1- create the health endpoint to our app
    2- add minimal configuration
    3- basic
 */
object Application extends IOApp.Simple {

    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    override def run: IO[Unit] = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config => 
        EmberServerBuilder
            .default[IO]
            .withHost(config.host) 
            .withPort(config.port)
            .withHttpApp(HttpApi[IO].endpoints.orNotFound)
            .build
            .use(_ => IO.println("Server ready, OH NOES YES!") *> IO.never)
        }
        
}
