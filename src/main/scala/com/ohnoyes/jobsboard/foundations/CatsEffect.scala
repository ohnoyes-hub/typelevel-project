package com.ohnoyes.jobsboard.foundations

import cats.effect.IO

object CatsEffect {

  /*
    describing computations as values
   */

  // IO = data structure describing arbitrary computations (including side effects)
  val firstIO: IO[Int] = IO.pure(42)
  val delayedIO: IO[Int] = IO.apply{
    println("I am doing something important")
    67
  }

  def main(args: Array[String]): Unit = {

  }
}
