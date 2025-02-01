package com.ohnoyes.jobsboard.http.validation

import cats.* 
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import com.ohnoyes.jobsboard.domain.job.*
import java.net.URL
import scala.util.{Try, Success, Failure}
import com.ohnoyes.jobsboard.domain.auth.*
import com.ohnoyes.jobsboard.domain.user.*



object validators {

    sealed trait ValidationFailure(val errorMessage: String)
    case class EmptyField(fieldName: String) extends ValidationFailure(s"$fieldName is empty")
    case class InvalidUrl(fieldName: String) extends ValidationFailure(s"$fieldName is not valid url")
    case class InvalidEmail(fieldName: String) extends ValidationFailure(s"Oh no! $fieldName is not valid email")

    // empty field, invalid URL, invalid email,...

    type ValidationResult[A] = ValidatedNel[ValidationFailure, A]
            
    trait Validator[A] {
        def validate(value: A): ValidationResult[A]
    }

    val emailRegex =
        """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
        
    def validateRequired[A](field: A, fieldName: String)(required: A => Boolean): ValidationResult[A] =
        if (required(field)) field.validNel
        else EmptyField(fieldName).invalidNel

    def validateUrl(field: String, fieldName: String): ValidationResult[String] =
        Try(URL(field).toURI()) match {
            case Success(_) => field.validNel
            case Failure(_) => InvalidUrl(fieldName).invalidNel
        }
    
    def validatedEmail(field: String, fieldName: String): ValidationResult[String] = {
        if (emailRegex.findFirstIn(field).isDefined) field.validNel
        else InvalidEmail(fieldName).invalidNel
    }

    given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
        val JobInfo(
            company, // should not be empty
            title, // should not be empty
            description, // should not be empty
            externalUrl, // should be a valid URL
            remote, 
            location, // should not be empty
            salaryLow,
            salaryHi,
            currency,
            country,
            tags,
            image,
            seniority,
            other
        ) = jobInfo

        val validCompany = validateRequired(company, "company")(_.nonEmpty)
        val validTitle = validateRequired(title, "title")(_.nonEmpty)
        val validDescription = validateRequired(description, "description")(_.nonEmpty)
        val validExternalUrl = validateUrl(externalUrl, "externalUrl")
        val validLocation = validateRequired(location, "location")(_.nonEmpty)

        (
            validCompany, 
            validTitle, 
            validDescription, 
            validExternalUrl, 
            remote.validNel,
            validLocation,
            salaryLow.validNel,
            salaryHi.validNel,
            currency.validNel,
            country.validNel,
            tags.validNel,
            image.validNel,
            seniority.validNel,
            other.validNel
        ).mapN(JobInfo.apply) // ValidatedNel[ValidationFailure, JobInfo]
        
    }

    // create validators for 
    given loginInfoValidator: Validator[LoginInfo] = (loginInfo: LoginInfo) => {
        val valideUserEmail = validateRequired(loginInfo.email, "email")(_.nonEmpty)
            .andThen(e => validatedEmail(e, "email"))

        val validUserPassword = validateRequired(loginInfo.password, "password")(_.nonEmpty)
        (valideUserEmail, validUserPassword).mapN(LoginInfo.apply) // ValidatedNel[ValidationFailure, LoginInfo]
    }

    // NewUserInfo
    given newUserInfoValidator: Validator[NewUserInfo] = (newUserInfo: NewUserInfo) => {
        val validUserEmail = validateRequired(newUserInfo.email, "email")(_.nonEmpty)
            .andThen(e => validatedEmail(e, "email"))

        // TODO: add more password validation (restrictions)
        val validUserPassword = validateRequired(newUserInfo.password, "password")(_.nonEmpty)
        // ^^ 

        (
            validUserEmail,
            validUserPassword,
            newUserInfo.firstName.validNel,
            newUserInfo.lastName.validNel,
            newUserInfo.company.validNel
        ).mapN(NewUserInfo.apply)
    }

    // NewPasswordInfo
    given newPasswordInfoValidator: Validator[NewPasswordInfo] = (newPasswordInfo: NewPasswordInfo) => {
        val validOldPassword = validateRequired(newPasswordInfo.oldPassword, "old password")(_.nonEmpty)
        val validNewPassword = validateRequired(newPasswordInfo.newPassword, "new password")(_.nonEmpty)

        (
            validOldPassword,
            validNewPassword
        ).mapN(NewPasswordInfo.apply)
    }
}
