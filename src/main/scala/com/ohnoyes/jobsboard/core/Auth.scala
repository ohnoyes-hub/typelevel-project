package com.ohnoyes.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import tsec.mac.jca.HMACSHA256

import com.ohnoyes.jobsboard.domain.security.*
import com.ohnoyes.jobsboard.domain.auth.*
import com.ohnoyes.jobsboard.domain.user.*

trait Auth[F[_]] {
    def login(email: String, password: String): F[Option[JwtToken]] // typesafe JWT token
    def signup(newUserInfo : NewUserInfo): F[Option[User]]
    def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]  
}

class LiveAuth[F[_]: MonadCancelThrow: Logger] private (users: Users[F], authenticator: Authenticator[F]) extends Auth[F] {
    override def login(email: String, password: String): F[Option[JwtToken]] = ???
    override def signup(newUserInfo : NewUserInfo): F[Option[User]] = ???
    override def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]] = ???
}

object LiveAuth {
    def apply[F[_]: MonadCancelThrow: Logger](users: Users[F], authenticator: Authenticator[F]): F[LiveAuth[F]] = new LiveAuth[F](users, authenticator).pure[F]
}