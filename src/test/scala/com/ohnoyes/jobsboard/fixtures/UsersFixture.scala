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

  val Riccardo = User(
    "riccardo@rockthejvm.com",
    "riccardorulez",
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )
}
