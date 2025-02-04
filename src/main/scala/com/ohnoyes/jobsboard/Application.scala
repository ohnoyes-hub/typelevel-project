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
import com.ohnoyes.jobsboard.modules.*
import pureconfig.ConfigSource
import com.ohnoyes.jobsboard.config.*
import com.ohnoyes.jobsboard.config.syntax.*
import com.typesafe.config.Config
import pureconfig.error.ConfigReaderException
import org.http4s.server.middleware.ErrorAction.httpApp


/* 
TODO: 
    1- create the health endpoint to our app
    2- add minimal configuration
    3- basic
 */
object Application extends IOApp.Simple {

    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap { 
        case AppConfig(postgresConfig, emberConfig, securityConfig, tokenConfig, emailServiceConfig) => 
            val appResource = for {
                xa <- Database.makePostgresResource[IO](postgresConfig)
                core <- Core[IO](xa, tokenConfig, emailServiceConfig)
                httpApi <- HttpApi[IO](core, securityConfig)
                server <- EmberServerBuilder
                        .default[IO]
                        .withHost(emberConfig.host) 
                        .withPort(emberConfig.port)
                        .withHttpApp(httpApi.endpoints.orNotFound)
                        .build
            } yield server

            appResource.use(_ => IO.println("Server ready, OH NOES YES!") *> IO.never)
    }
        
}
