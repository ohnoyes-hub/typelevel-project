package com.ohnoyes.jobsboard.core

import cats.effect.*
import cats.implicits.*
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.Message
import javax.mail.Transport

import com.ohnoyes.jobsboard.config.*

trait Emails[F[_]] {
    def sendEmail(to: String, subject: String, content: String): F[Unit]
    def sendPasswordRecoveryEmail(to: String, token: String): F[Unit] 
}

class LiveEmails[F[_]: MonadCancelThrow] private (emailServiceConfig: EmailServiceConfig) extends Emails[F] {

    val host = emailServiceConfig.host
    val port = emailServiceConfig.port
    val user = emailServiceConfig.user
    val pass = emailServiceConfig.pass
    val frontendUrl = emailServiceConfig.frontendUrl

    
    override def sendEmail(to: String, subject: String, content: String): F[Unit] = {
        val messageResource = for {
            prop <- propResource
            auth <- authenticatorResource
            session <- createSesion(prop, auth)
            message <- createMessage(session)("thomas@ohnoyes.xyz", to, subject, content)
        } yield message

        messageResource.use(msg => Transport.send(msg).pure[F])
    }

    override def sendPasswordRecoveryEmail(to: String, token: String): F[Unit] = {
        val subject = "OhNoYes Password Recovery"
        val content = s"""
        <div sytle="
        font-family: sans-serif;
        padding: 20px;
        border: 1px solid black;
        line-height: 2;
        font-size: 20px;
        "
        >
        <h1>OhNoYes Password Recovery</h1>
        <p>Hey there!</p>
        <p> Your token is: $token </p>
        <p> Click <a href="$frontendUrl">here</a> to go to the application</p>
        <p>👉 from ohnoyes </p>
        </div>
        """

        sendEmail(to, subject, content)
    }
    
    val propResource: Resource[F, Properties] = {
        val prop = new Properties
        prop.put("mail.smtp.auth", true)
        prop.put("mail.smtp.starttls.enable", true)
        prop.put("mail.smtp.host", host)
        prop.put("mail.smtp.port", port)
        prop.put("mail.smtp.ssl.trust", host)
        Resource.pure(prop)
    }

    val authenticatorResource: Resource[F, Authenticator] = {
        Resource.pure(new Authenticator {
            override def getPasswordAuthentication: PasswordAuthentication = {
                new PasswordAuthentication(user, pass)
            }
        })
    }

    def createSesion(prop: Properties, auth: Authenticator): Resource[F, Session] = {
        Resource.pure(Session.getInstance(prop, auth))
    }

    def createMessage(session: Session)(from: String, to: String, subject: String, content: String): Resource[F, MimeMessage] = {
        val message = new MimeMessage(session)
        message.setFrom(from)
        message.setRecipients(Message.RecipientType.TO, to)
        message.setSubject(subject)
        message.setContent(content, "text/html; charset=utf-8")
        Resource.pure(message)
    }
}

object LiveEmails {
    def apply[F[_]: MonadCancelThrow](emailServiceConfig: EmailServiceConfig): F[LiveEmails[F]] = 
        new LiveEmails[F](emailServiceConfig).pure[F]
}