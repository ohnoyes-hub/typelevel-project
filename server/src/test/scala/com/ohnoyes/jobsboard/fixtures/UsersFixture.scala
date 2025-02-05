package com.ohnoyes.jobsboard.fixtures

import cats.effect.IO
import com.ohnoyes.jobsboard.core.Users
import com.ohnoyes.jobsboard.domain.user.*


trait UsersFixture {

  val mockedUsers: Users[IO] = new Users[IO] {
        override def find(email: String): IO[Option[User]] = 
            if (email == danielEmail) IO.pure(Some(Daniel))
            else IO(None)
        override def create(user: User): IO[String] = IO.pure(user.email)
        override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
        override def delete(email: String): IO[Boolean] = IO.pure(true)
    }

  val Daniel = User(
    "daniel@rockthejvm.com",
    "$2a$10$RUrc4HAIsSSb908/h7w0nO4gZn.NrGjROXHxFlxY9lX8dQTYxQcLW", // "rockthejvm"
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM"),
    Role.ADMIN
  )
  val danielEmail = Daniel.email
  val danielPassword = "rockthejvm"

  val Riccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$DDfeZDKeWIJiszswg7ESHurcqD8UtF1M1PeB5PzmcXMfOeVR2twi6", // "riccardorulez",
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )
  val riccardoEmail = Riccardo.email
  val riccardoPassword = "riccardorulez"

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$QozreAc71PCMwx.mNtd5Yuhv6o34LpbM1.dXAcecYRQFQeelcnckK", //"simplepassword",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )

  val UpdatedRiccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$y5YEk5BeFCYFOcvkdY0fCexGHE1GGe/sRpfHfz56V86ytqGSVqp/K", // "riccardorocks",
    Some("RICCARDO"),
    Some("CARDIN"),
    Some("Adobe"),
    Role.RECRUITER
  )

  val NewUserDaniel = NewUserInfo(
    danielEmail,
    danielPassword,
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM")
  )

  val NewUserRiccardo = NewUserInfo(
    riccardoEmail,
    riccardoPassword,
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM")
  )
}
