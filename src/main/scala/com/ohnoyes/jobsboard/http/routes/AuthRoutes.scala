package com.ohnoyes.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import cats.effect.* 
import cats.implicits.*
import org.typelevel.log4cats.Logger
import org.http4s.server.Router
import org.http4s.HttpRoutes
import org.http4s.*
import tsec.authentication.asAuthed
import tsec.authentication.SecuredRequestHandler
import tsec.authentication.TSecAuthService

import com.ohnoyes.jobsboard.http.validation.syntax.*
import com.ohnoyes.jobsboard.http.responses.*
import com.ohnoyes.jobsboard.http.validation.syntax.*
import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.domain.auth.*
import com.ohnoyes.jobsboard.domain.user.*
import com.ohnoyes.jobsboard.domain.security.*

import scala.language.implicitConversions


class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F]{
  
    private val authenticator = auth.authenticator
    private val securedHandler: SecuredRequestHandler[F, String, User, JwtToken] = SecuredRequestHandler(authenticator)

    // POST /auth/login { LoginInfo } => 200 Ok with JWT as Authorization: Bearer {jwt}
    private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root / "login" => 
            req.validate[LoginInfo] { loginInfo =>
                val maybeJwtToken = for {
                    maybeToken <- auth.login(loginInfo.email, loginInfo.password)
                    _ <- Logger[F].info(s"User logging in ${loginInfo.email}")
                } yield maybeToken
                
                maybeJwtToken.map {
                    case Some(jwt) => authenticator.embed(Response(Status.Ok), jwt) // Autherization: Bearer {jwt}
                    case None => Response(Status.Unauthorized)
                }
            }
        } 

    // POST /auth/signup { NewUserInfo } => 201 Created or BadRequest
    private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root / "signup" => 
            req.validate[NewUserInfo] { newUserInfo =>
                for {
                    mayberNewUser <- auth.signup(newUserInfo)
                    resp <- mayberNewUser match {
                        case Some(user) => Created(user.email)
                        case None => BadRequest(s"Oh no! User email ${newUserInfo.email} already exists")
                    }
                } yield resp
            }
        }  

    // PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer {jwt} } => 200 Ok 
    private val changePasswordRoute: AuthRoute[F] = {
        case req @ PUT -> Root / "users" / "password" asAuthed user => 
            req.request.validate[NewPasswordInfo] { newPasswordInfo =>
                for {
                    maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo)
                    resp <- maybeUserOrError match {
                        case Right(Some(_)) => Ok()
                        case Right(None) => NotFound(FailureResponse(s"Oh no! User ${user.email} not found.")) // this should never happen
                        case Left(_) => Forbidden()
                    }
                } yield resp
            }
        }  

    // POST /auth/logout { Authorization: Bearer {jwt} } => 200 Ok
    private val logoutRoute: AuthRoute[F] = {
        case req @ POST -> Root / "logout" asAuthed _ => 
            val token = req.authenticator
            for {
                _ <- authenticator.discard(token)
                resp <- Ok()
            } yield resp
        } 

    // DELETE /auth/users/
    private val deleteUserRoutes: AuthRoute[F] = {
        case req @ DELETE -> Root / "users" / email asAuthed user => 
            auth.delete(email).flatMap {
                case true => Ok()
                case false => NotFound()
            }
        }  

    val unauthRoutes = loginRoute <+> createUserRoute
    val authRoutes = securedHandler.liftService(
        // TSecAuthService(changePasswordRoute.orElse(logoutRoute).orElse(deleteUserRoutes))
        changePasswordRoute.restrictedTo(allRoles) |+|
        logoutRoute.restrictedTo(allRoles) |+|
        deleteUserRoutes.restrictedTo(adminOnly)
    )


    val routes = Router(
        "/auth" -> (unauthRoutes <+> authRoutes)
    )
}

object AuthRoutes {
    def apply[F[_]: Concurrent: Logger](auth: Auth[F]): AuthRoutes[F] = 
        new AuthRoutes[F](auth)
}
