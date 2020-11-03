import $ivy.`com.akmetiuk::thera:0.2.0-M1`

import java.util.Date
import java.text.SimpleDateFormat

import os._
import thera._

case class Post(file: Path, date: Date) {
  lazy val htmlName: String = s"${file.baseName}.html"
  lazy val url: String = s"/posts/$htmlName"
  lazy val dateStr: String = Post.dateFormatter.format(date)
  lazy val src: String = read(file)
  lazy val title: String = Thera.split(src) match {
    case (header, _) =>
      ValueHierarchy.yaml(header)("title").asStr.value
  }
  lazy val asValue: Value = ValueHierarchy.names(
    "date"  -> Str(dateStr),
    "url"   -> Str(url),
    "title" -> Str(title),
  )
}

object Post {
  val dateParser    = new SimpleDateFormat("yyyy-MM-dd"    )
  val dateFormatter = new SimpleDateFormat("MMM dd, yyyy")

  def fromPath(f: Path): Post = {
    val postName = """(\d{4}-\d{2}-\d{2})-.*\.md""".r
    f.toIO.getName match { case postName(dateStr) => Post(
      file = f
    , date = dateParser.parse(dateStr)) }
  }
}
