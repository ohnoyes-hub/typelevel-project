package com.ohnoyes.jobsboard.playground

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.Message
import javax.mail.Transport


import cats.effect.* 
import com.ohnoyes.jobsboard.core.* 
import com.ohnoyes.jobsboard.config.*

object EmailsPlayground {
    def main(args: Array[String]): Unit = {
        // configs
        // user, pass, host, port
        /* 
        Host 	smtp.ethereal.email
        Port 	587
        Security 	STARTTLS
        Username 	annamarie39@ethereal.email
        Password 	7HZ3zZ1vuSXc3rvP2W
         */
        val host = "smtp.ethereal.email"
        val port = 587
        val user = "annamarie39@ethereal.email"
        val pass = "7HZ3zZ1vuSXc3rvP2W"
        val token = "ABCD1234"
        val frontendUrl = "https://google.com"

        // properties file
        /*
        mail.smtp.auth=true
        mail.smtp.starttls.enable=true
        mail.smtp.host=host
        mail.smtp.port=port
        mail.smtp.trust=host
         */
        val prop = new Properties
        prop.put("mail.smtp.auth", true)
        prop.put("mail.smtp.starttls.enable", true)
        prop.put("mail.smtp.host", host)
        prop.put("mail.smtp.port", port)
        prop.put("mail.smtp.ssl.trust", host)

        // authentication
        val auth = new Authenticator {
            override def getPasswordAuthentication: PasswordAuthentication = {
                new PasswordAuthentication(user, pass)
            }
        }

        // session
        val session = Session.getInstance(prop, auth)

        // email itself
        val subject = "Email from OhNoYes Typelevel"
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

        // message - MIME message
        val message = new MimeMessage(session)
        message.setFrom("daniel@rockthejvm.com")
        message.setRecipients(Message.RecipientType.TO, "the.user@gmail.com")
        message.setSubject(subject)
        message.setContent(content, "text/html; charset=utf-8")

        // send email
        Transport.send(message)
    }
}

object EmailsEffectPlayground extends IOApp.Simple {
    override def run: IO[Unit] = for {
        emails <- LiveEmails[IO](
            EmailServiceConfig(
                host = "smtp.ethereal.email",
                port = 587,
                user = "annamarie39@ethereal.email",
                pass = "7HZ3zZ1vuSXc3rvP2W",
                frontendUrl = "https://google.com"
                )
            )
        _ <- emails.sendPasswordRecoveryEmail("someone@ohnoyes.com", "OHNOYES1234")
    } yield ()
}
