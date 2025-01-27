package com.ohnoyes.jobsboard.core

import cats.*
import cats.implicits.*
import com.ohnoyes.jobsboard.domain.job.*
import java.util.UUID
import doobie.*
import doobie.implicits.* 
import doobie.util.* 
import doobie.postgres.implicits.*

trait  Jobs[F[_]] {
    // "algebra"
    // CRUD operations
    def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
    def all(): F[List[Job]]
    def find(id: UUID): F[Option[Job]]
    def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
    def delete(id: UUID): F[Int]
}

/*
id: UUID,
date: Long,
ownerEmail: String,
company: String,
title: String,
description: String,
externalUrl: String,
remote: Boolean,
location: String,
salaryLow: Option[Int],
salaryHi: Option[Int],
currency: Option[String],
country: Option[String],
tags: Option[List[String
image: Option[String],
seniority: Option[String],
other: Option[String],
active: Boolean
 */

class LiveJobs[F[_]: Applicative] private (xa: Transactor[F]) extends Jobs[F] {
    override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] = ???
    override def all(): F[List[Job]] = 
        sql"""
            SELECT
                id
                date
                ownerEmail
                company
                title
                description
                externalUrl
                remote
                location
                salaryLow
                salaryHi
                currency
                country
                tags
                image
                seniority
                other
                active
            FROM jobs
        """
        .query[Job]
        .to[List]
        .transact(xa)

    override def find(id: UUID): F[Option[Job]] = ???
    override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] = ???
    override def delete(id: UUID): F[Int] = ???
}

object LiveJobs {
    given jobRead: Read[Job] = Read[(
        UUID, // id
        Long, // date
        String, // ownerEmail
        String, // company
        String, // title
        String, // description
        String, // externalUrl
        Boolean, // remote
        String, // location
        Option[Int], // salaryLow
        Option[Int], // salaryHi
        Option[String], // currency
        Option[String], // country
        Option[List[String]], // tags
        Option[String], // image
        Option[String], // seniority
        Option[String], // other
        Boolean // active
    )].map {
        case(
            id: UUID,
            date: Long,
            ownerEmail: String,
            company: String,
            title: String,
            description: String,
            externalUrl: String,
            remote: Boolean,
            location: String,
            salaryLow: Option[Int],
            salaryHi: Option[Int],
            currency: Option[String],
            country: Option[String],
            tags: Option[List[String]],
            image: Option[String],
            seniority: Option[String],
            other: Option[String],
            active: Boolean
        ) => Job (
            id = id,
            date = date,
            ownerEmail = ownerEmail,
            JobInfo(
                company = company,
                title = title,
                description = description,
                externalUrl = externalUrl,
                remote = remote,
                location = location,
                salaryLow = salaryLow,
                salaryHi = salaryHi,
                currency = currency,
                country = country,
                tags = tags,
                image = image,
                seniority = seniority,
                other = other,
            ),
            active = active
        )
    }

    def apply[F[_]: Applicative](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]
}
