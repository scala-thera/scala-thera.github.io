import $ivy.`com.akmetiuk::thera:0.2.0-M3`

import $file.post, post._
import $file.util, util._

import os._
import thera._, ValueHierarchy.names

// 1.
val allPosts: List[Post] = walk(
    path = src/"posts",
    skip = p => p.ext !="md"
  ).map(Post.fromPath).toList

// 2.
val defaultCtx: ValueHierarchy =
  ValueHierarchy.yaml(read(src/"data"/"data.yml"))

// 3.
def htmlFragmentCtx(implicit ctx: => ValueHierarchy): ValueHierarchy =
  names("htmlFragment" ->
    Function.function[Str] { name =>
      val containsJs = Set(
        "google-tag-manager-head",
      )

      var source = read(src/s"fragments"/s"${name.value}.html")
      if (containsJs(name.value)) source = Thera.quote(source)
      Thera(source).mkValue(ctx).asStr
    }
  )

// 4.
val postTemplate = Thera(read(src/"templates"/"post.html"))
val defaultTemplate = Thera(read(src/"templates"/"default.html"))


// === Build procedure ===
def build(): Unit = {
  if (exists(compiled))
    list(compiled).foreach(remove.all)
  genStaticAssets()
  genCss()
  genPosts()
  genIndex()
  cleanup()
}

// 5.
def genStaticAssets(): Unit = {
  println("Copying static assets")
  for (f <- List("assets","favicon.png"))
    copy(src/f, compiled/f,
      replaceExisting = true, createFolders = true)
}

// 6.
def genCss(): Unit = {
  println("Processing CSS assets")
  implicit val ctx = defaultCtx + names(
    "cssAsset" -> Function.function[Str] { name =>
      Str(read(src/s"private-assets"/"css"/s"${name.value}.css")) }
  )

  val css = Thera(read(src/"private-assets"/"css"/"all.css")).mkString
  writeFile(compiled/"assets"/"all.css", css)
}

// 7.
def genPosts(): Unit = {
  println(s"Processing ${allPosts.length} posts...")

  for ( (post, idx) <- allPosts.zipWithIndex ) {
    println(s"[$idx/${allPosts.length}] Processing ${post.file}")
    val (header, body) = Thera.split(post.src)
    val postHtml = Thera.quote(postMarkdownToHtml(body)) // the post's HTML body as to interpreted as HTML, not a template
    val postThera = Thera(Thera.join(header, postHtml))

    implicit lazy val ctx: ValueHierarchy =
      defaultCtx + defaultTemplate.context +
      postTemplate.context +
      postThera.context +
      names(
        "date" -> Str(post.dateStr),
        "url" -> Str(post.url),
      ) + htmlFragmentCtx

    val result = pipeThera(postThera, postTemplate, defaultTemplate) // pipe the different Thera templates to pass the parameters up the hierarchy
    writeFile(compiled/"posts"/post.htmlName, result)
  }
}

// 8.
def genIndex(): Unit = {
  println("Generating index.html")
  val index = Thera(read(src/"index.html"))
  implicit lazy val ctx: ValueHierarchy =
    defaultCtx + defaultTemplate.context +
    index.context + htmlFragmentCtx + names(
      "allPosts" -> Arr(allPosts.sortBy(_.date) // show the most recent posts first
        .reverse.map(_.asValue))
    )

  val res = pipeThera(index, defaultTemplate)
  writeFile(compiled/"index.html", res)
}

// 9.
def cleanup(): Unit =
  remove.all(compiled/"code")

build() // actually run the build procedure
