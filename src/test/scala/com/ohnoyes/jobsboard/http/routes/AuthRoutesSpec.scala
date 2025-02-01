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

import scala.concurrent.duration.*

import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.fixtures.* 
import com.ohnoyes.jobsboard.domain.auth.*
import com.ohnoyes.jobsboard.domain.user.*
import com.ohnoyes.jobsboard.domain.security.*
import tsec.jws.mac.JWTMac
import org.http4s.headers.Authorization


class AuthRoutesSpec 
    extends AsyncFreeSpec 
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with UsersFixture {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////        
    // prep
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    val mockedAuthenticator: Authenticator[IO] = {
        // key for hashing
        val key = HMACSHA256.unsafeGenerateKey
        // identity store to retrieve users
        val idStore: IdentityStore[IO, String, User] = (email: String) => 
            if (email == danielEmail) OptionT.pure(Daniel)
            else if (email == riccardoEmail) OptionT.pure(Riccardo)
            else OptionT.none
        // jwt authenticator
        JWTAuthenticator.unbacked.inBearerToken(
            1.day, // expiry of token
            None, // max idle time (optional)
            idStore, // identity store
            key // hash key
        )
    }

    val mockedAuth: Auth[IO] = new Auth[IO] {
        def login(email: String, password: String): IO[Option[JwtToken]] = ???
        def signup(newUserInfo : NewUserInfo): IO[Option[User]] = ???
        def changePassword(
            email: String, 
            newPasswordInfo: NewPasswordInfo
        ): IO[Either[String, Option[User]]] = ???
    }


    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
    val authRoutes: HttpRoutes[IO] = AuthRoutes(mockedAuth).routes 

    extension (r: Request[IO]) 
        def withBearerToken(a: JwtToken): Request[IO] =
            r.putHeaders{
                val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
                // Authorization: Bearer {jwt}
                Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
            }

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
                // assertions hear
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
                // assertions hear
                response.status shouldBe Status.Ok
                response.headers.get(ci"Authorization") shouldBe defined
            }
        }

        "should return a 400 - Bad Request if the user created already exist" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/login")
                    .withEntity(NewUserDaniel)
                )
            } yield {
                // assertions hear
                response.status shouldBe Status.BadRequest
            }
        }

        "should return a 201 - Created if the user creation succeeds" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/login")
                    .withEntity(NewUserRiccardo)
                )
            } yield {
                // assertions hear
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
                // assertions hear
                response.status shouldBe Status.Ok
            }
        }

        "should return a 401 - Unauthorized if logging out without a valid JWT" in {
            for {
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/auth/logout")
                )
            } yield {
                // assertions hear
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
                // assertions hear
                response.status shouldBe Status.NotFound
            }
        }
        // change password - user exists but wrong old password => 403 Forbidden
        "should return a 403 - Forbidden if old password is forbidden" in {
            for {
                jwtToken <- mockedAuthenticator.create(riccardoEmail)
                response <- authRoutes.orNotFound.run(
                    Request(method = Method.PUT, uri = uri"/auth/users/password")
                    .withBearerToken(jwtToken)
                    .withEntity(NewPasswordInfo("wrongpassword", "newpassword"))
                )
            } yield {
                // assertions hear
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
                jwtToken <- mockedAuthenticator.create(riccardoEmail)
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
    }
}
