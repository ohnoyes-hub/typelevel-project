package com.ohnoyes.jobsboard.http.routes

import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams

import com.ohnoyes.jobsboard.domain.job.*
import com.ohnoyes.jobsboard.domain.pagination.*
import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.fixtures.* 

import java.util.UUID
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import java.{util => ju}


class JobRoutesSpec 
    extends AsyncFreeSpec 
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture 
    with SecuredRouteFixture {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////        
    // prep
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    val jobs: Jobs[IO] = new Jobs[IO] {
        override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] = 
            IO.pure(NewJobUuid)

        override def all(): fs2.Stream[IO, Job] = 
            fs2.Stream.emit(AwesomeJob)

        override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] = 
            if (filter.remote) IO.pure(List())
            else IO.pure(List(AwesomeJob))

        override def find(id: UUID): IO[Option[Job]] = 
            if (id == AwesomeJobUuid) IO.pure(Some(AwesomeJob))
            else IO.pure(None)

        override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] = 
            if (id == AwesomeJobUuid) IO.pure(Some(UpdatedAwesomeJob))
            else IO.pure(None)

        override def activate(id: UUID): IO[Int] = IO.pure(1)

        override def delete(id: UUID): IO[Int] = 
            if (id == AwesomeJobUuid) IO.pure(1)
            else IO.pure(0)

        override def possibleFilters(): IO[JobFilter] = IO(defaultFiler)
    }   

    val stripe: Stripe[IO] = new LiveStripe[IO](
        "key",
        "price",
        "https://example.com/success",
        "https://example.com/fail",
        "secret"
    ) {
        override def createCheckoutSession(jobId: String, userEmail: String): IO[Option[Session]] = 
            IO.pure(Some(Session.create(SessionCreateParams.builder().build())))

        override def handleWebhookEvent[A](payload: String, signature: String, action: String => IO[A]): IO[Option[A]] =
            IO.pure(None)
    }

    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
    val jobRoutes: HttpRoutes[IO] = JobsRoutes[IO](jobs, stripe).routes // what is being tested

    val defaultFiler: JobFilter = JobFilter(companies = List("Awesome Company"))
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////        
    // tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    "JobRoutes" - {
        "should return a job with a given id" in {
            // code under test
            for {
                // simulate an HTTP request
                response <- jobRoutes.orNotFound.run(
                    Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
                )
                // get the HTTP response
                retrieved <- response.as[Job]
                // make some assertions
            } yield {
                response.status shouldBe Status.Ok
                retrieved shouldBe AwesomeJob
            }
        }

        "should return all jobs" in {
            for {
                response <- jobRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/jobs")
                        .withEntity(JobFilter())
                )
                retrieved <- response.as[List[Job]]
            } yield {
                response.status shouldBe Status.Ok
                retrieved shouldBe List(AwesomeJob)
            }
        }

        "should return all jobs that satisfy a filter" in {
            for {
                response <- jobRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/jobs")
                        .withEntity(JobFilter(remote = true))
                )
                retrieved <- response.as[List[Job]]
            } yield {
                response.status shouldBe Status.Ok
                retrieved shouldBe List()
            }
        }

        "should create a new job" in {
            for {
                jwtToken <- mockedAuthenticator.create(danielEmail)
                response <- jobRoutes.orNotFound.run(
                    Request[IO](method = Method.POST, uri = uri"/jobs/create")
                        .withEntity(AwesomeJob.jobInfo)
                        .withBearerToken(jwtToken)
                )
                retrieved <- response.as[UUID]
            } yield {
                response.status shouldBe Status.Created
                retrieved shouldBe NewJobUuid
            }
        }

        "should only update a job that exist" in {
            for {
                jwtToken <- mockedAuthenticator.create(danielEmail)
                responseOk <- jobRoutes.orNotFound.run(
                    Request[IO](method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
                        .withEntity(UpdatedAwesomeJob.jobInfo)
                        .withBearerToken(jwtToken)
                )
                responseInvalid <- jobRoutes.orNotFound.run(
                    Request[IO](method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
                        .withEntity(UpdatedAwesomeJob.jobInfo)
                        .withBearerToken(jwtToken)
                )
            } yield {
                responseOk.status shouldBe Status.Ok
                responseInvalid.status shouldBe Status.NotFound
            }
        }

        "should forbid the update a job that the JWT token does not 'owns'" in {
            for {
                jwtToken <- mockedAuthenticator.create("someone@yahoo.com")
                response <- jobRoutes.orNotFound.run(
                    Request[IO](method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
                        .withEntity(UpdatedAwesomeJob.jobInfo)
                        .withBearerToken(jwtToken)
                )                
            } yield {
                response.status shouldBe Status.Unauthorized
            }
        }

        "should only delete a job that exist" in {
            for {
                jwtToken <- mockedAuthenticator.create(danielEmail)
                responseOk <- jobRoutes.orNotFound.run(
                    Request[IO](method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
                        .withBearerToken(jwtToken)
                )
                responseInvalid <- jobRoutes.orNotFound.run(
                    Request[IO](method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
                    .withBearerToken(jwtToken)
                )
            } yield {
                responseOk.status shouldBe Status.Ok
                responseInvalid.status shouldBe Status.NotFound
            }
        }

        "should surface all possible filters" in {
            for {
                response <- jobRoutes.orNotFound.run(
                    Request(method = Method.GET, uri = uri"/jobs/filters")
                )
                filter <- response.as[JobFilter]
            } yield {
                filter shouldBe defaultFiler
            }
        }
    }
}
