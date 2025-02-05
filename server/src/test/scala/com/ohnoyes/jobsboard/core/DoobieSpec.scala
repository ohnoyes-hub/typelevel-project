package com.ohnoyes.jobsboard.core

import cats.effect.* 
import doobie.* 
import doobie.implicits.* 
import doobie.util.* 
import org.testcontainers.containers.PostgreSQLContainer
import doobie.hikari.HikariTransactor

trait DoobieSpec {

    // TODO: implemented by whatever test case interacts with the DB
    val initScript: String
    
    // simulate a database
    // docker container
    // testContainers
    val postgress: Resource[IO, PostgreSQLContainer[Nothing]] = {
        val acquire = IO {
            val container: PostgreSQLContainer[Nothing] = 
                new PostgreSQLContainer("postgres").withInitScript(initScript)
            container.start()
            container
        }
        val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
        Resource.make(acquire)(release)
    }

    // set up a Postgres transactor
    val transactor: Resource[IO, Transactor[IO]] = for {
        db <- postgress
        ce <- ExecutionContexts.fixedThreadPool[IO](1)
        xa <- HikariTransactor.newHikariTransactor[IO](
            "org.postgresql.Driver",
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword(),
            ce
        )
    } yield xa
}
