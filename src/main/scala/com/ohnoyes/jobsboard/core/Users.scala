package com.ohnoyes.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.* 
import doobie.util.* 
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger

import com.ohnoyes.jobsboard.domain.user.* 

trait Users[F[_]] {
  // CRUD
  def find(email: String): F[Option[User]]
  def create(user: User): F[String] // return identifier
  def update(user: User): F[Option[User]]
  def delete(user: User): F[Boolean]
}

final class LiveUsers[F[_]: MonadCancelThrow: Logger] private(xa: Transactor[F]) extends Users[F] {
  override def find(email: String): F[Option[User]] = ???
  override def create(user: User): F[String] = ???
  override def update(user: User): F[Option[User]] = ???
  override def delete(user: User): F[Boolean] = ???
}

object LiveUsers {
  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveUsers[F]] = 
    new LiveUsers[F](xa).pure[F]
}

