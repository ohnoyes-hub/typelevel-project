package com.ohnoyes.jobsboard.core

import cats.*
import cats.effect.*
import cats.data.OptionT
import cats.implicits.*
import org.typelevel.log4cats.Logger
import tsec.authentication.AugmentedJWT
import tsec.mac.jca.HMACSHA256
import tsec.authentication.JWTAuthenticator
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import tsec.authentication.IdentityStore
import tsec.authentication.BackingStore
import tsec.common.SecureRandomId

import scala.concurrent.duration.*

import com.ohnoyes.jobsboard.domain.security.*
import com.ohnoyes.jobsboard.domain.auth.*
import com.ohnoyes.jobsboard.domain.user.*
import com.ohnoyes.jobsboard.config.*

trait Auth[F[_]] {
    def login(email: String, password: String): F[Option[User]] // typesafe JWT token
    def signup(newUserInfo : NewUserInfo): F[Option[User]]
    def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]  
    def delete(email: String): F[Boolean]
    // password recovery
    def sendPasswordRecoveryToken(email: String): F[Unit]
    def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean]
}

class LiveAuth[F[_]: Async: Logger] private (users: Users[F]) extends Auth[F] {
    override def login(email: String, password: String): F[Option[User]] = for {
        // find user in the DB -> return None if no user
        maybeUser <- users.find(email)
        // otherwise, check if the password is correct
        // Option[User].filter(User => IO[Boolean]) => IO[Option[User]] which is problem
        // Option[User].filterA(User => G[Boolean]) => G[Option[User]]
        maybeValidatedUser <- maybeUser.filterA(user => BCrypt
                .checkpwBool[F](
                    password, 
                    PasswordHash[BCrypt](user.hashedPassword)
                )
            )
    } yield maybeValidatedUser

    override def signup(newUserInfo : NewUserInfo): F[Option[User]] = 
        // find the user in the db, if we did => return None
        users.find(newUserInfo.email).flatMap { maybeUser =>
            maybeUser match {
                case Some(_) => None.pure[F]
                case None => for {
                    // hash the new password
                    hashedPassword <- BCrypt.hashpw[F](newUserInfo.password)
                    // create a new user in the db
                    user <- User(
                        newUserInfo.email,
                        hashedPassword,
                        newUserInfo.firstName,
                        newUserInfo.lastName,
                        newUserInfo.company,
                        Role.RECRUITER
                    ).pure[F]
                    // create a new user in the db
                    _ <- users.create(user)
                } yield Some(user)
            }
        }
         

    override def changePassword(
        email: String, 
        newPasswordInfo: NewPasswordInfo
    ): F[Either[String, Option[User]]] = {
        def updateUser(user: User, newPassword: String): F[Option[User]] = for {
            hashedPassword <- BCrypt.hashpw[F](newPasswordInfo.newPassword)
            updatedUser <- users.update(user.copy(hashedPassword = hashedPassword))
        } yield updatedUser

        def checkAndUpdate(user: User, oldPassword: String, newPassword: String): F[Either[String, Option[User]]] = for {
            // check password
            passCheck <- BCrypt
                .checkpwBool[F](
                    newPasswordInfo.oldPassword, 
                    PasswordHash[BCrypt](user.hashedPassword)
                )
            updateResult <- 
                if (passCheck) {
                    updateUser(user, newPassword).map(Right(_))
                } else Left("Invalid password").pure[F]
        } yield updateResult

        users.find(email).flatMap { 
            case None => Right(None).pure[F]
            case Some(user) => 
                val NewPasswordInfo(oldPassword, newPassword) = newPasswordInfo
                checkAndUpdate(user, oldPassword, newPassword)
        }
    }

    override def delete(email: String): F[Boolean] = 
        users.delete(email)

    // password recovery
    override def sendPasswordRecoveryToken(email: String): F[Unit] = ???
    
    override def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean] = ???
}

object LiveAuth {
    def apply[F[_]: Async: Logger](users: Users[F]): F[LiveAuth[F]] = {
        new LiveAuth[F](users).pure[F]
    }
}