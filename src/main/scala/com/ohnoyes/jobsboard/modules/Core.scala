package com.ohnoyes.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.config.*

final class Core[F[_]] private (val jobs: Jobs[F], val users: Users[F],  val auth: Auth[F])


// postgres -> jobs -> core -> httpApi -> app
object Core {
    def apply[F[_]: Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] = {
        val coreF = for {
            jobs <- LiveJobs[F](xa)
            users <- LiveUsers[F](xa)
            auth <- LiveAuth[F](users)
        } yield new Core(jobs, users, auth)
        Resource.eval(coreF)
    }

}
