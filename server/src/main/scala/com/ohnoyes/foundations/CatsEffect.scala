package com.ohnoyes.foundations

import cats.{Defer, MonadError}
import cats.effect.kernel.Deferred
import cats.effect.{Concurrent, Fiber, GenSpawn, IO, IOApp, MonadCancel, Ref, Resource, Spawn, Sync, Temporal}

import java.io.{File, FileWriter, PrintWriter}
import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.concurrent.duration.*

object CatsEffect extends IOApp.Simple {

  /*
    describing computations as values
   */

  // IO = data structure describing arbitrary computations (including side effects)
  val firstIO: IO[Int] = IO.pure(42)
  val delayedIO: IO[Int] = IO{
    // complex code
    println("I am doing something important")
    67
  }

  def evaluateIO[A](io: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global // "platform"
    val meaningOfLife = io.unsafeRunSync()
    println(s"the meaning of life is $meaningOfLife")
  }

  // transformations for IO
  // map + flatMap
  val improvedMeaningOfLife = firstIO.map(_ * 2)
  val printedMeaningOfLife = firstIO.flatMap(mol => IO(println(s"meaning of life is $mol")))
  // for-comprehensions
  def smallProgram(): IO[Unit] = for {
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _ <- IO(println(s"you said $line1 and $line2"))
  } yield ()

  // old style of standard Scala apps
//  def main(args: Array[String]): Unit = {
//    evaluateIO(smallProgram())
//  }

  // raise/"catch" errors
  val aFailure: IO[Int] = IO.raiseError(new RuntimeException("oh noes, a proper failure"))
  val dealWithIt = aFailure.handleErrorWith { // try-recover
    case _: RuntimeException => IO(println("we recovered!"))
  }

  // fibers = "lightweight threads"
  // *> = "and then"
  val delayedPrint = IO.sleep(1.second) *> IO(println("delayed print"))
  val manyPrints = for {
    fib1 <- delayedPrint.start
    fib2 <- delayedPrint.start
    _ <- fib1.join // join by blocking the calling fiber (fib1 joins the main fiber)
    _ <- fib2.join
  } yield ()
  /*
  join semantically block the calling fiber until the target fiber has completed its execution. When you join a fiber, you are essentially waiting for the result of that fiber's computation without blocking any actual threads.
  The join method is then called on this fiber, which waits for the fiber to complete and retrieves its result. The main fiber continues executing other operations (like printing to the console) without being blocked by the join.
   */

  val cancelledFiber = for {
    fib <- delayedPrint.onCancel(IO(println("I was cancelled"))).start // this takes 1 second to run
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel // this takes 500 milliseconds to run, so it will cancel the fiber
    _ <- fib.join
  } yield ()

  // uncancelation
  val ignoredCancellation = for {
    fib <- IO.uncancelable(_ => delayedPrint.onCancel(IO(println("I was cancelled")))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()

  // resources
  val readingResource = Resource.make(
    IO(scala.io.Source.fromFile("src/main/scala/com/ohnoyes/jobsboard/foundations/CatsEffect.scala"))
  ) (source => IO(println("closing resource")) *> IO(source.close()))
  val readingEffect = readingResource.use(source => IO(source.getLines().foreach(println)))

  // compose resources
  val copiedFileResource = Resource.make(IO(new PrintWriter(
    new FileWriter(new File("src/main/resources/dumpedFile.txt"))))
  ) { writer =>
    IO(println("closing writer")) *> IO(writer.close())
  }
  val compositeResource = for {
    source <- readingResource
    destination <- copiedFileResource
  } yield (source, destination)

  val copyFileEffect = compositeResource.use {
    case (source, destination) => IO(source.getLines().foreach(line => destination.println(line)))
  }

  // abstract kinds of computations

  // MonadCancel = cancelable computations
  trait MyMonadCancell[F[_], E] extends MonadError[F, E] {
    trait CancellationFlagResetter { // aka Poll
      def apply[A] (fa: F[A]): F[A] // with the cancellation flag reset
    }
    def canceled: F[Unit]
    def uncancelable[A](poll: CancellationFlagResetter => F[A]): F[A]
  }

  // MonadCancel for IO
  val monadCancellIO: MonadCancel[IO, Throwable] = MonadCancel[IO]
  val uncancelableIO = monadCancellIO.uncancelable(_ => IO(42)) // same as IO.uncancellable(...)

  // Spawn = ability to create fibers
  trait MyGenSpawn[F[_], E] extends MonadCancel[F, E] {
    def start[A](fa: F[A]): F[Fiber[F, E, A]] // creates a fiber
  }

  trait MySpawn[F[_]] extends GenSpawn[F, Throwable]

  val spawnIO = Spawn[IO]
  val fiber = spawnIO.start(delayedPrint) // creates a fiber, same as delayedPrint.start

  // Concurrent = concurrency primitives(atomic references + promises)
  trait MyConcurrent[F[_]] extends Spawn[F] {
    def ref[A](a: A): F[Ref[F, A]]
    def deferred[A]: F[Deferred[F, A]]
  }

  // Temporal = ability to suspend computations for a given time
  trait MyTemporal[F[_]] extends Concurrent[F] {
    def sleep(duration: FiniteDuration): F[Unit]
  }

  // Sync = ability to suspend synchronous arbitrary expressions in an effect
  trait MySync[F[_]] extends MonadCancel[F, Throwable] with Defer[F]{
    def delay[A](thunk: => A): F[A]
    def blocking[A](thunk: => A): F[A] // runs on a dedicated blocking thread pool
  }

  // Asynce = ability to suspend asynchronous computations (i.e. on other thread pools) into an effect managed by CE
  trait MyAsync[F[_]] extends Sync[F] with Temporal[F] {
    def executionContext: F[ExecutionContext]
    def async[A](cb: (Either[Throwable, A] => Unit) => F[Option[F[Unit]]]): F[A]

  }

  // CE apps have a "run" method returning an IO, which will internally be evaluated in a main function
  override def run = copyFileEffect
}
