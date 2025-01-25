package com.ohnoyes.foundations

import java.awt.Container

object Cats {

  /*
  type classes
  - Applicative
  - Functor
  - FlatMap
  - Monad
  - ApplicativeError/MonadError
   */

  // functor - "mappleble" structures
  trait MyFunctor[F[_]] {
    def map[A, B](initialValue: F[A])(f: A => B): F[B]
  }

  import cats.Functor
  import cats.instances.list._ // this is already in Cats scope without this import
  val listFunctor = Functor[List]
  val incrementedNumbers = listFunctor.map(List(1, 2, 3))(_ + 1)

  // generalizable "mappable" APIs
  def increment[F[_]](container: F[Int])(using functor: Functor[F]): F[Int] =
    functor.map(container)(_ + 1)

  import cats.syntax.functor._ // import the map extension method
  def increment_v2[F[_]](container: F[Int])(using functor: Functor[F]): F[Int] =
    container.map(_ + 1)

  // applicative - pure, wrap existing values into "wrapper" values
  trait MyApplicative[F[_]] extends Functor[F] {
      def pure[A](value: A): F[A]
  }

  import cats.Applicative
  val applicativeList = Applicative[List]
  val aSimpleList = applicativeList.pure(2)
  import cats.syntax.applicative._ // pure extension method
  val aSimpleList_v2 = 2.pure[List]

  // FlatMap
  trait MyFlatMap[F[_]] extends Functor[F] {
      def flatMap[A, B](initialValue: F[A])(f: A => F[B]): F[B]
  }

  import cats.FlatMap
  val flatMapList = FlatMap[List]
  val flatMappedList = flatMapList.flatMap(List(1, 2, 3))(x => List(x, x + 1))
  import cats.syntax.flatMap._ // flatMap extension method
  def crossProduct[F[_]: FlatMap, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] =
    containerA.flatMap(a => containerB.map(b => (a, b)))

  def crossProduct_v2[F[_] : FlatMap, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] =
    for {
      a <- containerA
      b <- containerB
    } yield (a, b)

  // Monad - applicative + flatMap
  trait MyMonad[F[_]] extends Applicative[F] with FlatMap[F] {
    override def map[A, B](initialValue: F[A])(f: A => B): F[B] =
      flatMap(initialValue)(a => pure(f(a)))
  }

  import cats.Monad
  val monadList = Monad[List]
  def crossProduct_v3[F[_]: Monad, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] =
    for {
      a <- containerA
      b <- containerB
    } yield (a, b)

  // ApplicativeError - computation that can fail
  trait MyApplicativeError[F[_], E] extends MyApplicative[F] {
      def raiseError[A](error: E): F[A]
  }

  import cats.ApplicativeError
  type ErrorOr[A] = Either[String, A]
  val applicativeErrorEither = ApplicativeError[ErrorOr, String]
  val desiredValue: ErrorOr[Int] = applicativeErrorEither.pure(42)
  val failedValue: ErrorOr[Int] = applicativeErrorEither.raiseError("Something went wrong")
  import cats.syntax.applicativeError._ // raiseError extension method
  val failedValue_v2: ErrorOr[Int] = "Something went wrong".raiseError

  // MonadError - Monad + ApplicativeError
  trait MyMonadError[F[_], E] extends ApplicativeError[F, E] with Monad[F] {
    // no need to implement map or flatMap
  }
  import cats.MonadError
  val monadErrorEither = MonadError[ErrorOr, String]


  def main(args: Array[String]): Unit = {

  }
}
