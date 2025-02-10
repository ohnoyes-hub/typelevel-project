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


class AuthRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (auth: Auth[F], authenticator: Authenticator[F]) extends HttpValidationDsl[F]{
  
    // POST /auth/login { LoginInfo } => 200 Ok with JWT as Authorization: Bearer {jwt}
    private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root / "login" => 
            req.validate[LoginInfo] { loginInfo =>
                val maybeJwtToken = for {
                    maybeUser <- auth.login(loginInfo.email, loginInfo.password)
                    _ <- Logger[F].info(s"User logging in ${loginInfo.email}")
                    maybeToken <- maybeUser.traverse(user => authenticator.create(user.email))
                } yield maybeToken
                
                maybeJwtToken.map {
                    case Some(jwt) => authenticator.embed(Response(Status.Ok), jwt) // Autherization: Bearer {jwt}
                    case None => Response(Status.Unauthorized)
                }
            }
        } 

    // POST /auth/users { NewUserInfo } => 201 Created or BadRequest
    private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root / "users" => 
            req.validate[NewUserInfo] { newUserInfo =>
                for {
                    mayberNewUser <- auth.signup(newUserInfo)
                    resp <- mayberNewUser match {
                        case Some(user) => Created(user.email)
                        case None => 
                            BadRequest(FailureResponse(s"Oh no! User email ${newUserInfo.email} already exists"))
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

    // POST /auth/reset { ForgotPasswordInfo }
    private val forgotPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root / "reset" => 
            for {
                fogotPasswordInfo <- req.as[ForgotPasswordInfo]
                _ <- auth.sendPasswordRecoveryToken(fogotPasswordInfo.email)
                resp <- Ok()
            } yield resp
        }

    // POST /auth/recover { RecoverPasswordInfo }
    private val recoverPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root / "recover" => 
            for {
                rpInfo <- req.as[RecoverPasswordInfo]
                recoverySuccess <- auth.recoverPasswordFromToken(rpInfo.email, rpInfo.token, rpInfo.newPassword)
                resp <- if (recoverySuccess) Ok() else Forbidden(FailureResponse("Oh no! Password recovery failed as email and token do not match."))
            } yield resp
        }

    val unauthRoutes = loginRoute <+> createUserRoute <+> forgotPasswordRoute <+> recoverPasswordRoute
    val authRoutes = SecuredHandler[F].liftService(
        // TSecAuthService(changePasswordRoute.orElse(logoutRoute).orElse(deleteUserRoutes))
        changePasswordRoute.restrictedTo(allRoles) |+|
        logoutRoute.restrictedTo(allRoles) |+|
        deleteUserRoutes.restrictedTo(adminOnly)
    )


    val routes = Router(
        "/auth" -> (unauthRoutes <+> authRoutes)
    )
}

/*
- need a CAPABILITY, instead of instead of intermediate values (use dependency injection like with SecuredHandler)
    - instantiated ONCE in the entire application
*/

object AuthRoutes {
    def apply[F[_]: Concurrent: Logger: SecuredHandler](
        auth: Auth[F], 
        authenticator: Authenticator[F]
    ): AuthRoutes[F] = 
        new AuthRoutes[F](auth, authenticator)
}
