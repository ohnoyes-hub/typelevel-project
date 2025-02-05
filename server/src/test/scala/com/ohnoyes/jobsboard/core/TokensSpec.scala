package com.ohnoyes.jobsboard.core

import cats.effect.*
import cats.effect.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

import com.ohnoyes.jobsboard.fixtures.*
import com.ohnoyes.jobsboard.domain.user.*
import com.ohnoyes.jobsboard.config.*

class TokensSpec extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with UsersFixture {
    val initScript: String = "sql/recoverytokens.sql"
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    "Tokens 'algebra'" - {
        "should not create a new token for a non-existing user" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10000000L))
                    maybeToken <- tokens.getToken("somebody@fakemail.com")
                } yield maybeToken

                program.asserting(_ shouldBe None)
            }
        }

        "should create a token for an existing user" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10000000L))
                    maybeToken <- tokens.getToken(danielEmail)
                } yield maybeToken

                program.asserting(_ shouldBe defined)
            }
        }

        "should not validate expired tokens" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100L))
                    maybeToken <- tokens.getToken(danielEmail)
                    _ <- IO.sleep(500.millis)
                    isTokenValid <- maybeToken match {
                        case Some(token) => tokens.checkToken(danielEmail, token)
                        case None => IO.pure(false)
                    }
                } yield isTokenValid

                program.asserting(_ shouldBe false)
            }
        }
  
        "should validate tokens that are not yet expired" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10000000L))
                    maybeToken <- tokens.getToken(danielEmail)
                    isTokenValid <- maybeToken match {
                        case Some(token) => tokens.checkToken(danielEmail, token)
                        case None => IO.pure(false)
                    }
                } yield isTokenValid

                program.asserting(_ shouldBe true)
            }
        }

         "should only validate tokens for user that generated them" in {
            transactor.use { xa =>
                val program = for {
                    tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10000000L))
                    maybeToken <- tokens.getToken(danielEmail)
                    isDanielTokenValid <- maybeToken match {
                        case Some(token) => tokens.checkToken(danielEmail, token)
                        case None => IO.pure(false)
                    }
                    isOtherTokenValid <- maybeToken match {
                        case Some(token) => tokens.checkToken("somebody@fakemail.com", token)
                        case None => IO.pure(false)
                    }
                } yield (isDanielTokenValid, isOtherTokenValid)

                program.asserting{ case (isDanielTokenValid, isOtherTokenValid) =>
                    isDanielTokenValid shouldBe true
                    isOtherTokenValid shouldBe false
                }
            }
        }


    }
}