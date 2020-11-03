import scala.util.Try
import cats._, cats.implicits._, cats.data._, cats.effect._

package object effectextensions {
  // start snippet Ef
  type Ef[A] = EitherT[IO, String, A]
  // end snippet Ef

  // start snippet RichWrappers
  implicit def showExn: Show[Throwable] = Show.show[Throwable] { e =>
    s"${e.getMessage}\n${e.getStackTrace.mkString("\n")}"
  }

  implicit class RichEither[E: Show, A](x: Either[E, A]) {
    def etr: Ef[A] = EitherT.fromEither[IO](x).leftMap(_.show)
  }

  implicit class RichDelayed[A](x: => A) {
    def sus: Ef[A] = EitherT.right[String](IO { x })
    def exn: Ef[A] = Try(x).toEither.etr
  }
  // end snippet RichWrappers

  // start snippet RichEf
  implicit class RichEf[A](ef: Ef[A]) {
    def run: A = ef.value.unsafeRunSync().bimap(
      err => throw new RuntimeException(s"Error Happened:\n$err")
    , res => res).merge
  }
  // end snippet RichEf
}
