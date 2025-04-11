package com.ohnoyes.jobsboard.modules

import cats.effect.*
import cats.data.*
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.AugmentedJWT
import tsec.mac.jca.HMACSHA256
import tsec.authentication.JWTAuthenticator
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import tsec.authentication.IdentityStore
import tsec.authentication.BackingStore
import tsec.common.SecureRandomId
import tsec.authentication.SecuredRequestHandler

import com.ohnoyes.jobsboard.core.*
import com.ohnoyes.jobsboard.config.*
import com.ohnoyes.jobsboard.http.routes.*
import com.ohnoyes.jobsboard.domain.security.*
import com.ohnoyes.jobsboard.domain.user.*



class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]) {
    given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
    private val healthRoutes = HealthRoutes[F].routes 
    private val jobsRoutes = JobsRoutes[F](core.jobs, core.stripe).routes
    private val authRoutes = AuthRoutes[F](core.auth, authenticator).routes

    val endpoints = Router(
        "/api" -> (healthRoutes <+> jobsRoutes <+> authRoutes)
    )
}

object HttpApi {

    def createAuthenticator[F[_]: Sync](users: Users[F], securityConfig: SecurityConfig): F[Authenticator[F]] = {
        // 1. Identity store: String => OptionT[F, User]

        val idStore: IdentityStore[F, String, User] = (email: String) => 
            OptionT(users.find(email))

        // 2. backing store for JWT tokens for JWT tokens: BackingStore[F, id, JwtToken]
        val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
            new BackingStore[F, SecureRandomId, JwtToken] {
                // REF (atomic)
                override def get(id: SecureRandomId): OptionT[F, JwtToken] = 
                    OptionT(ref.get.map(_.get(id))) // OptionT(/*/*F[JwtToken*/
                override def put(elem: JwtToken): F[JwtToken] = 
                    ref.modify(store => (store + (elem.id -> elem), elem))
                override def update(v: JwtToken): F[JwtToken] = 
                    put(v)
                override def delete(id: SecureRandomId): F[Unit] = 
                    ref.modify(store => (store - id, ()))
            }
        }

        // 3. hash key
        val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8")) 

        // 4. authenticator
        for { // val authenticatorF: F[Authenticator[F]] = 
                key <- keyF
                tokenStore <- tokenStoreF
                
        } yield JWTAuthenticator.backed.inBearerToken(
                    expiryDuration = securityConfig.jwtExpiryDuration, 
                    maxIdle = None,
                    identityStore = idStore, 
                    tokenStore = tokenStore,
                    signingKey = key
                )
    }

    def apply[F[_]: Async: Logger](core: Core[F], securityConfig: SecurityConfig): Resource[F, HttpApi[F]] = 
        Resource
            .eval(createAuthenticator(core.users, securityConfig))
            .map(authenticator => new HttpApi[F](core, authenticator))
}
