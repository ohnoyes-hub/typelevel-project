package com.ohnoyes.jobsboard.http.routes

import cats.effect.* 
import cats.implicits.*
import org.typelevel.log4cats.Logger

import com.ohnoyes.jobsboard.http.validation.syntax.*
import com.ohnoyes.jobsboard.core.*
import org.http4s.server.Router
import org.http4s.HttpRoutes


class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F]{
  
    // POST /auth/login { LoginInfo } => 200 Ok with JWT as Authorization: Bearer {jwt}
    private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case POST -> Root / "login" => 
            Ok("TODO")
        } 

    // POST /auth/signup { NewUserInfo } => 201 Created
    private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case POST -> Root / "signup" => 
            Ok("TODO")
        }  

    // PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer {jwt} } => 200 Ok 
    private val changePasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case PUT -> Root / "users" / "password" => 
            Ok("TODO")
        }  

    // POST /auth/logout { Authorization: Bearer {jwt} } => 200 Ok
    private val logoutRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case POST -> Root / "logout" => 
            Ok("TODO")
        }   


    val routes = Router(
        "/auth" -> (loginRoute  <+> createUserRoute <+> changePasswordRoute <+> logoutRoute)
    )
}

object AuthRoutes {
    def apply[F[_]: Concurrent: Logger](auth: Auth[F]): AuthRoutes[F] = 
        new AuthRoutes[F](auth)
}
