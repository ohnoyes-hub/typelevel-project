package com.ohnoyes.jobsboard.core

trait Stripe {
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
}
