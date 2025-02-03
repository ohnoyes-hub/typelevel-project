package com.ohnoyes.jobsboard.core

import cats.effect.*
import cats.data.OptionT
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator


import scala.concurrent.duration.*

import com.ohnoyes.jobsboard.fixtures.*
import com.ohnoyes.jobsboard.domain.user.*
import com.ohnoyes.jobsboard.domain.security.*
import com.ohnoyes.jobsboard.domain.auth.*
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import com.ohnoyes.jobsboard.config.SecurityConfig


class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UsersFixture {  
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    private val mockedUsers: Users[IO] = new Users[IO] {
        override def find(email: String): IO[Option[User]] = 
            if (email == danielEmail) IO.pure(Some(Daniel))
            else IO(None)
        override def create(user: User): IO[String] = IO.pure(user.email)
        override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
        override def delete(email: String): IO[Boolean] = IO.pure(true)
    }

    val mockedConfig = SecurityConfig("secret", 1.day)

    // val mockedAuthenticator: Authenticator[IO] = {
    //     // key for hashing
    //     val key = HMACSHA256.unsafeGenerateKey
    //     // identity store to retrieve users
    //     val idStore: IdentityStore[IO, String, User] = (email: String) => 
    //         if (email == danielEmail) OptionT.pure(Daniel)
    //         else if (email == riccardoEmail) OptionT.pure(Riccardo)
    //         else OptionT.none
    //     // jwt authenticator
    //     JWTAuthenticator.unbacked.inBearerToken(
    //         1.day, // expiry of token
    //         None, // max idle time (optional)
    //         idStore, // identity store
    //         key // hash key
    //     )
    // }


    "Auth 'algebra'" - {
        "login should return None if the user does not exist" in {
            val program = for {
                auth <- LiveAuth[IO](mockedUsers)
                maybToken <- auth.login("user@invalid.com", "password")
            } yield maybToken

            program.asserting(_ shouldBe None)
        }

        "login should return None if the user exist but the password is wrong" in {
            val program = for {
                auth <- LiveAuth[IO](mockedUsers)
                maybToken <- auth.login(danielEmail, "wrongpassword")
            } yield maybToken

            program.asserting(_ shouldBe None)
        }

        "login should return a token if the user exist and the passwork is correct" in {
            val program = for {
                auth <- LiveAuth[IO](mockedUsers)
                maybToken <- auth.login(danielEmail, "rockthejvm") 
            } yield maybToken

            program.asserting(_ shouldBe defined)
        }

        "signing up should not create a user with an existing email" in {
            val program = for {
                auth <- LiveAuth[IO](mockedUsers)
                maybeUser <- auth.signup(NewUserInfo(
                    danielEmail, 
                    "SomePassword", 
                    Some("Daniel"), 
                    Some("Lastname"), 
                    Some("Company")
                ))
            } yield maybeUser

            program.asserting(_ shouldBe None)
        }

        "signing up should create a completely new user" in {
            val program = for {
                auth <- LiveAuth[IO](mockedUsers)
                maybeUser <- auth.signup(NewUserInfo(
                    "bob@gmail.com", 
                    "somePassword", 
                    Some("Bob"), 
                    Some("Builder"), 
                    Some("Some Company")
                ))
            } yield maybeUser

            program.asserting {
                case Some(user) => 
                    user.email shouldBe "bob@gmail.com"
                    user.firstName shouldBe Some("Bob")
                    user.lastName shouldBe Some("Builder")
                    user.company shouldBe Some("Some Company")
                    user.role shouldBe Role.RECRUITER
                case _ => fail("User was not created")
            }
        }

        "changePassword should return a Right(None) if user does not exist" in {
            val program = for {
                auth <- LiveAuth[IO](mockedUsers)
                result <- auth.changePassword("alice@gmail.com", NewPasswordInfo("oldPassword", "newPassword"))
            } yield result

            program.asserting(_ shouldBe Right(None))
        }
        
        "changePassword should return Left with an error if the user exist and the password is incorrect" in {
            val program = for {
                auth <- LiveAuth[IO](mockedUsers)
                result <- auth.changePassword(danielEmail, NewPasswordInfo("oldPassword", "newPassword"))
            } yield result

            program.asserting(_ shouldBe Left("Invalid password"))
        }

        "changePassword should correctly change password if all details are correct" in {
            val program = for {
                auth <- LiveAuth[IO](mockedUsers)
                result <- auth.changePassword(danielEmail, NewPasswordInfo("rockthejvm", "newPassword"))
                isNicePassword <- result match {
                    case Right(Some(user)) => 
                        BCrypt.checkpwBool[IO](
                            "newPassword", PasswordHash[BCrypt](user.hashedPassword)
                        )
                    case _ => IO.pure(false)
                }
            } yield isNicePassword

            program.asserting(_ shouldBe true)
        }
    }
}
