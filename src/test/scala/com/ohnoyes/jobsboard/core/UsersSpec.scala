package com.ohnoyes.jobsboard.core

import cats.effect.*

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import com.ohnoyes.jobsboard.fixtures.*

class UsersSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with UsersFixture {
    override val initScript: String = "sql/users.sql"
  
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    "Users 'algebra'" - {
        "should retrieve a user by email" in {
            transactor.use { xa =>
                val program = for {
                    users <- LiveUsers[IO](xa)
                    //retrieved <- users.find("riccardo@rockthejvm.com")
                } yield ()
                program.asserting(_ shouldBe ())
            }
        }
    }
}