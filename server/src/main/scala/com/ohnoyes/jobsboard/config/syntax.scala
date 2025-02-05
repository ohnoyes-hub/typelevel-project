package com.ohnoyes.jobsboard.config

import pureconfig.ConfigSource
import pureconfig.ConfigReader
import cats.MonadThrow
import cats.implicits.*
import pureconfig.error.ConfigReaderException
import scala.reflect.ClassTag


object syntax {
    extension (source: ConfigSource)
        def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] =
            F.pure(source.load[A]).flatMap { // value => value match { // F[Either[Error, A]]
                case Left(errors) => F.raiseError[A](ConfigReaderException[A](errors))
                case Right(value) => F.pure(value)
            }
}