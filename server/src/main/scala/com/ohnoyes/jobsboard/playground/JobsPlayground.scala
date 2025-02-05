package com.ohnoyes.jobsboard.playground

import cats.effect.* 
import doobie.* 
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor
import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.domain.job.*
import scala.io.StdIn
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


object JobsPlayground extends IOApp.Simple {

    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  
    val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
        ec <- ExecutionContexts.fixedThreadPool(16)
        xa <- HikariTransactor.newHikariTransactor[IO](
            "org.postgresql.Driver",
            "jdbc:postgresql:board",
            "docker",
            "docker",
            ec
        )
    } yield xa

    val jobInfo = JobInfo.minimal(
        company = "ScalaDev",
        title = "Scala Developer",
        description = "We are looking for a Scala Developer",
        externalUrl = "https://www.scala.io",
        remote = true,
        location = "Remote"
    )

    override def run: IO[Unit] = postgresResource.use { xa =>
        for {
            jobs <- LiveJobs[IO](xa)
            _ <- IO(println("Ready, Next...")) *> IO(StdIn.readLine)
            id <- jobs.create("thomas@ohnoyes.xyz", jobInfo)
            _ <- IO(println("Next...")) *> IO(StdIn.readLine)
            list <- jobs.all()
            _ <- IO(println(s"All jobs: $list. Next...")) *> IO(StdIn.readLine)
            _ <- jobs.update(id, jobInfo.copy(title = "Scala Developer (Remote)"))
            newJob <- jobs.find(id)
            _ <- IO(println(s"New job: $newJob. Next...")) *> IO(StdIn.readLine)
            _ <- jobs.delete(id)
            listAfter <- jobs.all()
            _ <- IO(println(s"All jobs after delete: $listAfter. Next...")) *> IO(StdIn.readLine)
        } yield ()
    }
}
