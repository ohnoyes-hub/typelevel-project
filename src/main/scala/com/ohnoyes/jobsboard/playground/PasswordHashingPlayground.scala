package com.ohnoyes.jobsboard.playground

import cats.effect.IOApp
import cats.effect.IO
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash

object PasswordHashingPlayground extends IOApp.Simple {
  override def run = 
    BCrypt.hashpw[IO]("newPassword").flatMap( IO.println) *>
    BCrypt.checkpwBool[IO](
        "newPassword", PasswordHash[BCrypt]("$2a$10$PMGWSb3M3RcKX4650M901OReKdwADKlFfLIKOVwrDQeM4yeZDQl5G")
    ).flatMap(IO.println)
}
