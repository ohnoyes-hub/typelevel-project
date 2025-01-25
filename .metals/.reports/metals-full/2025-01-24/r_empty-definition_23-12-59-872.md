error id: `<none>`.
file://<WORKSPACE>/src/main/scala/com/ohnoyes/jobsboard/foundations/Http4s.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
|empty definition using fallback
non-local guesses:
	 -

Document text:

```scala
package com.ohnoyes.jobsboard.foundations

import cats._
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import org.http4s.headers.*
import cats.Monad
import cats.effect.* //{IO, IOApp}
import org.http4s.* // HttpRoutes
import org.http4s.dsl.* //Http4sDsl
import org.http4s.dsl.impl.*//{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s.server.*
import org.typelevel.ci.CIString
import org.http4s.ember.server.EmberServerBuilder

import java.util.UUID

object Http4s extends IOApp.Simple {

  // simulate an HTTP server with "students" and "courses"
  type Student = String
  case class Instructor(name: String, course: String)
  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepository {
    // a "database"
    val catsEffectCourse = Course(
      "63343418-af07-4e3a-a468-29e489e264aa",
      "The Big Course",
      2025,
      List("Alice", "Bob", "Charlie"),
      "John Doe"
    )

    val courses: Map[String, Course] = Map(
      catsEffectCourse.id -> catsEffectCourse
    )

    // API
    def findCoursesById(courseId: UUID): Option[Course] =
      courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }

  // essential REST endpoints
  // GET localhost:8080/courses?instructor=John%20Doe&year=2025
  // GET localhost:8080/courses/63343418-af07-4e3a-a468-29e489e264aa/students

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")
  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    // Validated in Cats under the hood
    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match{
          case Some(y) => y.fold(
            _ => BadRequest("Parameter 'year' is invalid"),
            year => Ok(courses.filter(_.year == year).asJson)
          )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match {
          case Some(students) => Ok(students.asJson, Header.Raw(CIString("My-Custom-Header"), "ohnoyes"))
          case None => NotFound(s"No course with id $courseId found")
        }
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("I am healthy!")
    }
  }

  // combining the routes:
  def allRoutes[F[_]: Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  def routerWithPathPrefixes = Router(
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
    .withHttpApp(routerWithPathPrefixes  /*courseRoutes[IO].orNotFound*/)
    .build
    .use(_ => IO("Server ready!") *> IO.never)
}

```

#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.