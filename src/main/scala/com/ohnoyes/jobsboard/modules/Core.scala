package com.ohnoyes.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import com.ohnoyes.jobsboard.core.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

final class Core[F[_]] private (val jobs: Jobs[F])


// postgres -> jobs -> core -> httpApi -> app
object Core {
    def apply[F[_]: Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] =
        Resource
            .eval(LiveJobs[F](xa))
            .map(jobs => new Core(jobs))
}
