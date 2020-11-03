import $ivy.`com.akmetiuk::thera:0.2.0-M1`

import thera._
import os._


val src      = pwd/"src"
val compiled = pwd/"_site"

/**
 * Command line pipe. Invokes an external application, obtains its
 * input and output streams and feeds
 * the `input` to the output stream. Returns the contents of the
 * input stream of the command.
 */
def pipeIntoCommand(cmd: List[String], input: String, workdir: Path,
  encoding: String = "utf8"): String = {
  val p = proc(cmd).call(
    cwd = workdir,
    stdin = input
  )
  println(p.err.text)
  p.out.text
}

def postMarkdownToHtml(str: String): String =
  pipeIntoCommand(
    "pandoc" ::
    "--toc" ::
    "--webtex" ::
    "--template=../src/templates/pandoc-post.html" ::
    "--filter=../pandoc-filters/graphviz.py" ::
    "--filter=../pandoc-filters/plantuml.py" ::
    "--filter=../pandoc-filters/include-code.py" :: Nil,
    str, compiled)

def pandocRaw(str: String): String =
  pipeIntoCommand("pandoc" :: Nil, str, compiled)

def writeFile(f: Path, str: String): Unit =
  write.over(f, str,
    createFolders = true, truncate = false)

def pipeThera(tmls: Template*)(
  implicit ctx: ValueHierarchy): String =
  tmls.tail.foldLeft(tmls.head.mkValue) { (v, tml) =>
    tml.mkValue.asFunction(v :: Nil)
  }.asStr.value
