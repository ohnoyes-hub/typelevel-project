package com.ohnoyes.jobsboard.http.routes

import cats.effect.*
import cats.data.*
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.jws.mac.JWTMac

import scala.concurrent.duration.*

import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.fixtures.* 
import com.ohnoyes.jobsboard.domain.auth.*
import com.ohnoyes.jobsboard.domain.user.*
import com.ohnoyes.jobsboard.domain.security.*
import org.http4s.headers.Authorization


class AuthRoutesSpec 
    extends AsyncFreeSpec 
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with UsersFixture 
    with SecuredRouteFixture {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////        
    // prep
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    

    val mockedAuth: Auth[IO] = new Auth[IO] {
        def login(email: String, password: String): IO[Option[JwtToken]] = 
            if (email == danielEmail && password == danielPassword) 
                mockedAuthenticator.create(danielEmail).map(Some(_))
            else IO.pure(None)

        def signup(newUserInfo : NewUserInfo): IO[Option[User]] = 
            if (newUserInfo.email == riccardoEmail) 
                IO.pure(Some(Riccardo))
            else IO.pure(None)

        def changePassword(
            email: String, 
            newPasswordInfo: NewPasswordInfo
        ): IO[Either[String, Option[User]]] = 
            if (email == danielEmail)
                if (newPasswordInfo.oldPassword == danielPassword)
                    IO.pure(Right(Some(Daniel)))
                else IO.pure(Left("Invalid password"))
            else 
                IO.pure(Right(None))

        def delete(email: String): IO[Boolean] = IO.pure(true)

        def authenticator: Authenticator[IO] = mockedAuthenticator
    }


    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
    val authRoutes: HttpRoutes[IO] = AuthRoutes(mockedAuth).routes 

    

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////        
    // tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    "AuthRoutes" - {
        "should return a 401 - unauthorized if login fails" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/login")
                    .withEntity(LoginInfo(danielEmail, "wrongpassword"))
                )
            } yield {
                response.status shouldBe Status.Unauthorized
            }
        }

        "should return a 200 - OK and a JWT if login is successful" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/login")
                    .withEntity(LoginInfo(danielEmail, danielPassword))
                )
            } yield {
                response.status shouldBe Status.Ok
                response.headers.get(ci"Authorization") shouldBe defined
            }
        }

        "should return a 400 - Bad Request if the user created already exist" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/signup")
                    .withEntity(NewUserDaniel)
                )
            } yield {
                response.status shouldBe Status.BadRequest
            }
        }

        "should return a 201 - Created if the user creation succeeds" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/signup")
                    .withEntity(NewUserRiccardo)
                )
            } yield {
                response.status shouldBe Status.Created
            }
        }

        "should return a 200 - OK if logging out with a valid JWT" in {
            for {
                jwtToken <- mockedAuthenticator.create(danielEmail)
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/logout")
                    .withBearerToken(jwtToken)
                )
            } yield {
                response.status shouldBe Status.Ok
            }
        }

        "should return a 401 - Unauthorized if logging out without a valid JWT" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/logout")
                )
            } yield {
                response.status shouldBe Status.Unauthorized
            }
        }

        // change password - user doesn't exist => 404 Not Found
        "should return a 404 - Not Found if changing password for user that does not exist" in {
            for {
                jwtToken <- mockedAuthenticator.create(riccardoEmail)
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.PUT, uri = uri"/auth/users/password")
                    .withBearerToken(jwtToken)
                    .withEntity(NewPasswordInfo(riccardoPassword, "newpassword"))
                )
            } yield {
                response.status shouldBe Status.NotFound
            }
        }
        // change password - user exists but wrong old password => 403 Forbidden
        "should return a 403 - Forbidden if old password is incorrect" in {
            for {
                jwtToken <- mockedAuthenticator.create(danielEmail)
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.PUT, uri = uri"/auth/users/password")
                    .withBearerToken(jwtToken)
                    .withEntity(NewPasswordInfo("wrongpassword", "newpassword"))
                )
            } yield {
                response.status shouldBe Status.Forbidden
            }
        }
        // change password - user JWT is invalid => 401 Unauthorized
        "should return a 401 - Unauthorized if changing password with without a JWT" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.PUT, uri = uri"/auth/users/password")
                    .withEntity(NewPasswordInfo(danielPassword, "newpassword"))
                )
            } yield {
                // assertions hear
                response.status shouldBe Status.Unauthorized
            }
        }
        // change password - user exists and JWT is valid => 200 OK
        "should return a 200 - OK if changing password for a user with a valid JWT and password" in {
            for {
                jwtToken <- mockedAuthenticator.create(danielEmail)
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.PUT, uri = uri"/auth/users/password")
                    .withBearerToken(jwtToken)
                    .withEntity(NewPasswordInfo(danielPassword, "newpassword"))
                )
            } yield {
                // assertions hear
                response.status shouldBe Status.Ok
            }
        }

        "should return a 401 - Unauthorized if a non-admin tries to delete a user" in {
            for {
                jwtToken <- mockedAuthenticator.create(riccardoEmail)
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.DELETE, uri = uri"/auth/users/daniel@rockthejvm.com")
                    .withBearerToken(jwtToken)
                    .withEntity(NewPasswordInfo(danielPassword, "newpassword"))
                )
            } yield {
                // assertions hear
                response.status shouldBe Status.Unauthorized
            }
        }

        "should return a 200 - Ok if a admin tries to delete a user" in {
            for {
                jwtToken <- mockedAuthenticator.create(danielEmail)
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.DELETE, uri = uri"/auth/users/daniel@rockthejvm.com")
                    .withBearerToken(jwtToken)
                    .withEntity(NewPasswordInfo(danielPassword, "newpassword"))
                )
            } yield {
                // assertions hear
                response.status shouldBe Status.Ok
            }
        }
    }
}
