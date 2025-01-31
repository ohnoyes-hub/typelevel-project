package com.ohnoyes.jobsboard.domain

object auth {
  final case class LoginInfo(
    email: String,
    password: String
  )

  final case class NewPasswordInfo( // email needs to already be authenticated
    oldPassword: String,
    newPassword: String
  )
}
