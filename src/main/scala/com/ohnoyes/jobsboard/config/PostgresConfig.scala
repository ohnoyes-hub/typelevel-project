package com.ohnoyes.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class PostgresConfig(nThreads: Int, url: String, user: String, password: String)
    derives ConfigReader
