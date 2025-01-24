package com.ohnoyes.jobsboard.foundations

import cats.effect.kernel.MonadCancelThrow
import cats.effect.{IO, IOApp}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import doobie.implicits.*
import doobie.util.ExecutionContexts

object Doobie extends IOApp.Simple {

  // docker stuff
  /*
  docker ps # gets the name of the container (typelevel-project-db-1)
  docker exec -it typelevel-project-db-1 psql -U docker # connect to docker db
   */

  case class Student(id: Int, name: String)

  // transactor
  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // JDBC connector
    "jdbc:postgresql://localhost:5432/demo", // database URL
    "docker", // username
    "docker" // password
  )

  // read
  def findAllStudentNames: IO[List[String]] = {
    val query = sql"select name from students".query[String]
    val action = query.to[List]
    action.transact(xa)
  }

  // write
  def saveStudent(id: Int, name: String): IO[Int] = {
    val query = sql"insert into students (id, name) values ($id, $name)"
    val action = query.update.run
    action.transact(xa)
  }

  // read as case classes with fragments
  def findStudentsByInitial(letter: String): IO[List[Student]] = {
    val selectPart = fr"select id, name" // fr = fragment
    val fromPart = fr"from students"
    val wherePart = fr"where left(name, 1) = $letter" // left most character from the name field

    val statement = selectPart ++ fromPart ++ wherePart
    val action = statement.query[Student].to[List]
    action.transact(xa)
  }

  // organize code
  trait Students[F[_]] { // "repository"
    def findById(id: Int): F[Option[Student]]
    def findAll: F[List[Student]]
    def create(name: String): F[Int]
  }

  object Students {
    def make[F[_] : MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students[F] {
      def findById(id: Int): F[Option[Student]] =
        sql"select id, name from students where id = $id".query[Student].option.transact(xa)

      def findAll: F[List[Student]] =
        sql"select id, name from students".query[Student].to[List].transact(xa)

      def create(name: String): F[Int] =
        sql"insert into students (name) values ($name)".update.withUniqueGeneratedKeys[Int]("id").transact(xa)
    }
  }

  val postgressResource = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](8)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5432/demo",
      "docker",
      "docker",
      ce
    )
  } yield xa

  val smallProgram = postgressResource.use { xa =>
    val studentsRepo = Students.make(xa)
    for {
      id <- studentsRepo.create("alice")
      alice <- studentsRepo.findById(id)
      _ <- IO(println(s"The first student is $alice"))
    } yield ()
  }

  override def run = smallProgram
  //findStudentsByInitial("t").map(println)
  // saveStudent(3, "alice").map(println)
}
