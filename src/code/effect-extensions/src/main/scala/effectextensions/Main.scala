package effectextensions

// start snippet imports
import cats._, cats.implicits._, cats.data._, cats.effect._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import better.files._, better.files.File._, java.io.{ File => JFile }
// end snippet imports

trait Base {

  // start snippet requestHandler
  def requestHandler(requestBody: String): Ef[Unit] =
    for {
      bodyJson <- parse(requestBody)                   .etr
      _        <- println(s"Parsed body: $bodyJson")   .sus
      fileName <- bodyJson.hcursor.get[String]("file") .etr
      fileBody <- File(fileName).contentAsString       .exn
      _        <- println(s"Parsed file: $fileBody")   .sus
    } yield ()
  // end snippet requestHandler
}

object Successful extends Base with App {
  // start snippet Successful
  requestHandler("""{"file": "foo.txt"}""") .run
  // end snippet Successful
}

object CirceParserFailed extends Base with App {
  // start snippet CirceParserFailed
  requestHandler("""{"file": "foo.txt}""") .run
  // end snippet CirceParserFailed
}

object CirceKeyFailed extends Base with App {
  // start snippet CirceKeyFailed
  requestHandler("""{"stuff": "foo.txt"}""") .run
  // end snippet CirceKeyFailed
}

object FileFailed extends Base with App {
  // start snippet FileFailed
  requestHandler("""{"file": "stuff"}""") .run
  // end snippet FileFailed
}
