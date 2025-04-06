package com.ohnoyes.jobsboard.core

import cats.* 
import cats.implicits.*
import org.typelevel.log4cats.Logger

import com.stripe.{Stripe => TheStripe}
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams

import com.ohnoyes.jobsboard.logging.syntax.*
import com.ohnoyes.jobsboard.config.*

trait Stripe[F[_]] {
    /*
        1. User calls an endpoint on our server
            (send a JobInfo to us) - will be persisted to the database - Jobs[F].create(...)
        2. return a checkout page URL
        3. frontend redirects to that URL
        4. User pays(credit card details,...)
        5. backend will be notified by Stripe (webhook)
            - test mode: use Stripe CLI to redirect the events to localhost:727/api/jobs/webhook..
        6. Perform final operation on the job advert - set the active flag to false for that job id 

    app -> http -> stripe -> redirect user
                          <- user pays stripe
    activate job <- webhook <- stripe
    */
    def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]]
}

class LiveStripe[F[_]: MonadThrow: Logger](
    key: String,
    price: String,
    successUrl: String,
    cancelUrl: String,
) extends Stripe[F] {
    // globally set constant
    TheStripe.apiKey = key
    
    override def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]] = 
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            // automatic receipt/invoice
            .setInvoiceCreation(
                SessionCreateParams.InvoiceCreation.builder().setEnabled(true).build()
            )
            // save the user email
            .setPaymentIntentData(
                SessionCreateParams.PaymentIntentData.builder().setReceiptEmail(userEmail).build()
            )
            // redirect to the success page
            .setSuccessUrl(s"$successUrl/$jobId") // need config
            .setCancelUrl(s"$cancelUrl") // need config
            .setCustomerEmail(userEmail)
            .setClientReferenceId(jobId) // our reference id that will be sent to me by the webhook
            .addLineItem(
              SessionCreateParams.LineItem.builder()
                .setQuantity(1L) // 1 item
                // Provide the exact Price ID (for example, pr_1234) of the product you want to sell
                .setPrice("price_1R9BofAZMbULBEb9IH1PWCD8") // need config
                .build())
            .build()
            .pure[F]
            .map(params => Session.create(params)) // this is a blocking call, so we need to run it in a blocking context   
            .map(_.some)
            .logError(error => s"Oh no, checkout session creation failed: $error")
            .recover { case _ => None}
}

object LiveStripe {
    def apply[F[_]: MonadThrow: Logger](stripeConfig: StripeConfig): F[Stripe[F]] = 
        new LiveStripe[F](
            stripeConfig.key,
            stripeConfig.price,
            stripeConfig.successUrl,
            stripeConfig.cancelUrl
        ).pure[F]
}
