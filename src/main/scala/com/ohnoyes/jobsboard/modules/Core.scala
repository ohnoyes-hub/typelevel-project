package com.ohnoyes.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.config.*

final class Core[F[_]] private (val jobs: Jobs[F], val auth: Auth[F])


// postgres -> jobs -> core -> httpApi -> app
object Core {
    def apply[F[_]: Async: Logger](xa: Transactor[F])(securityConfig: SecurityConfig): Resource[F, Core[F]] = {
        val coreF = for {
            jobs <- LiveJobs[F](xa)
            users <- LiveUsers[F](xa)
            auth <- LiveAuth[F](users)(securityConfig)
        } yield new Core(jobs, auth)

        Resource.eval(coreF)
    }
        // Resource
        //     .eval(LiveJobs[F](xa))
        //     .map(jobs => new Core(jobs))
}
