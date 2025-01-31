package com.ohnoyes.jobsboard.fixtures

import cats.effect.IO
import com.ohnoyes.jobsboard.domain.user.*


trait UsersFixture {

  val Daniel = User(
    "daniel@rockthejvm.com",
    "rockthejvm",
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM"),
    Role.ADMIN
  )
  val danielEmail = Daniel.email
  val danielPassword = Daniel.hashedPassword

  val Riccardo = User(
    "riccardo@rockthejvm.com",
    "riccardorulez",
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )
  val riccardoEmail = Riccardo.email

  val NewUser = User(
    "newuser@gmail.com",
    "simplepassword",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )

  val UpdatedRiccardo = User(
    "riccardo@rockthejvm.com",
    "riccardorocks",
    Some("RICCARDO"),
    Some("CARDIN"),
    Some("Adobe"),
    Role.RECRUITER
  )

}
