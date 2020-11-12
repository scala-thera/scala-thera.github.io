# Creating a blog with Thera

In this tutorial, you will build a blog powered by [Thera](https://github.com/scala-thera/thera) and published on [GitHub Pages](https://pages.github.com).

By using Thera, you will be able to reduce code duplication, increase your productivity and better organize your project.

You can already have a look at the final result [here](https://scala-thera.github.io). The source code of the tutorial is hosted on [GitHub](https://github.com/scala-thera/scala-thera.github.io). You can use it to jump start your development.

## Prerequisites

To follow this tutorial, you need to:

1. Create a public GitHub repository; it will host the blog source code.
2. Create a [Docker](https://www.docker.com) repository; it will host the image used by [GitHub Actions](https://github.com/features/actions) to publish the website.
3. Install Docker on your machine; you will use it to develop locally and to deploy the blog's Docker image on Docker Hub.

## Structure

We will use [Ammonite](https://ammonite.io) and [os-lib](https://github.com/lihaoyi/os-lib) in conjunction with [Pandoc](https://pandoc.org) to setup the build procedure, with Thera streamlining the process. The procedure, driven by various Thera templates, will process assets, CSS files and posts (in Markdown) and generate the blog's HTML.

Here's the final project structure:

```
├── .github/workflows - GitHub Actions configuration
├── environment - Docker image and utilities
├── pandoc-filters - Filters for Pandoc
├── src
    ├── assets - Static assets
    ├── data - Blog's main metadata (title, description, etc.)
    ├── fragments - Reusable HTML fragments
    ├── posts - Posts in Markdown
    ├── private-assets/css - CSS files
    ├── templates - Main Thera templates
    ├── 404.html
    ├── favicon.png
    ├── index.html - Blog's entry-point
├── amm - Script that automatically downloads Ammonite
├── build.sc - Ammonite script for building the blog
├── post.sc - Ammonite script storing the Post Scala model
├── util.sc - Ammonite script storing utility functions
```

## Tutorial

The rest of the tutorial is organized as follows:

1. How to setup the Docker environment to run locally and deploy
2. How to setup Github Actions to deploy the blog on GitHub Pages
3. How to organize the blog's source files and Thera templates
4. How to build the blog with Thera

### Part 1: Docker environment

First of all, we setup the Dockerfile:

```docker
FROM hseeberger/scala-sbt:8u242_1.3.8_2.13.1

RUN apt-get update
RUN apt-get -y upgrade

# Pandoc, PlantUML, GraphViz
RUN apt-get install -y\
  pandoc python-pip plantuml graphviz\
  libgraphviz-dev graphviz-dev pkg-config
RUN pip install pandocfilters pygraphviz

# Start a server to browse the generated site
CMD (mkdir _site; cd _site && python -m SimpleHTTPServer 8888)
```

Then, we create a shell script `thera.sh` to run locally the Docker image for development:

```shell
#!/usr/bin/env bash

HELP="Usage: thera <command>, where <command> is one of:
  start - Start Thera
  stop  - Stop Thera
  restart - Restart Thera
  build - Run build.sc script on Thera under Ammonite
  help  - Show this message
  any other command - run that command on Thera Docker container (provided the container is running)."


SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SELF_DIR="${SCRIPT_DIR}/.."
PROJECT_NAME=my-project-name # you can replace it with your own project name
IMAGE_NAME="$PROJECT_NAME:latest"

function start_thera {
  echo "Building image $IMAGE_NAME"
  docker build --file "$SCRIPT_DIR/Dockerfile" \
    -t $IMAGE_NAME .

  echo "Starting $IMAGE_NAME"
  docker run -td \
    -v "$SELF_DIR:/root/$PROJECT_NAME" \
    -v "$HOME/.ivy2:/root/.ivy2" \
    -v "$HOME/.ammonite:/root/.ammonite" \
    -v "$HOME/.cache:/root/.cache" \
    -p 8888:8888 \
    --name "$PROJECT_NAME" \
    --rm \
    --workdir "/root/$PROJECT_NAME" \
    "$IMAGE_NAME"
}

function stop_thera {
  docker stop "$PROJECT_NAME"
}

function run_on_thera {
  docker exec -ti "$PROJECT_NAME" $@
}

function build_thera {
  run_on_thera ./amm build.sc
}

function restart_thera {
  stop_thera; start_thera
}

case $1 in
    start) start_thera;;
     stop) stop_thera;;
  restart) restart_thera;;
    build) build_thera;;

  '' | help) echo -e "$HELP";;

  *) run_on_thera $@;;
esac
```

Finally, we create a small shell script `deploy-docker-image.sh` to deploy our blog's image to Docker Hub. You should replace <your_Docker_username> and <your_project_Docker_repository> with the right information. The script will build the image, tag it with the current date and push it to Docker Hub:

```shell
#!/usr/bin/env bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

docker build --no-cache \
  --file "$SCRIPT_DIR/Dockerfile" \
  -t <your_Docker_username>/<your_project_Docker_repository>:$(date +%F) .
docker login
docker push <your_Docker_username>/<your_project_Docker_repository>:$(date +%F)
```

### Part 2: GitHub Actions

The code on GitHub should be organized as follows:

- `source` branch: the default branch storing the source code
- `master` branch: the branch hosting the built blog's assets

Our CI consists of two steps executed on pushes and pull requests:

1. A `test` step that verifies that the Docker image exists (and can be pulled), and that the website can be successfully built
2. A `publish_website` step that runs if `test` succeeds and publishes the blog on GitHub Pages.

To enable it, add the following `ci.yaml` file in a folder `.github/workflows` at the root of the project. You should replace <your_Docker_username>, <your_project_Docker_repository> and <tag_to_use> (a date, as we defined in `deploy-docker-image.sh`) with the right information:

```yaml
name: Website CI/CD

on:
  push:
    branches: source
  pull_request:
    branches: source

jobs:
  test:
    runs-on: ubuntu-latest
    container: <your_Docker_username>/<your_project_Docker_repository>:<tag_to_use>

    steps:
      - name: Git Checkout
        uses: actions/checkout@v2

      - name: Cache Ivy
        uses: actions/cache@v1.1.2
        with:
          path: /root/.ivy2/cache
          key: ${{ runner.os }}-ivy-${{ hashFiles('**/*.sc') }}
          restore-keys: ${{ runner.os }}-ivy-

      - name: Cache Coursier and Mill
        uses: actions/cache@v1.1.2
        with:
          path: /root/.cache
          key: ${{ runner.os }}-general-${{ hashFiles('**/*.sc') }}
          restore-keys: ${{ runner.os }}-general-

      - name: Build Website
        run: ./amm build.sc

  publish_website:
    runs-on: ubuntu-latest
    container: <your_Docker_username>/<your_project_Docker_repository>:<tag_to_use>
    needs: [test]
    if: github.event_name == 'push'

    steps:
      - name: Git Checkout
        uses: actions/checkout@v2

      - name: Cache Ivy
        uses: actions/cache@v1.1.2
        with:
          path: /root/.ivy2/cache
          key: ${{ runner.os }}-ivy-${{ hashFiles('**/*.sc') }}
          restore-keys: ${{ runner.os }}-ivy-

      - name: Cache Coursier and Mill
        uses: actions/cache@v1.1.2
        with:
          path: /root/.cache
          key: ${{ runner.os }}-general-${{ hashFiles('**/*.sc') }}
          restore-keys: ${{ runner.os }}-general-

      - name: Build Website
        run: ./amm build.sc

      - name: Deploy Website
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./_site
          publish_branch: master
```

Then, enable GitHub Pages for your repository in the repository settings, choosing `master` as the branch to build from.

### Part 3: Blog's source code

The blog's source code resides mainly in the `src/` folder, except some (optional) Pandoc filters used to generate images and include code in HTML stored in `pandoc-filters/`.

The blog's main data is stored in `data/data.yml`, you can update it as you wish:

```yaml
title: <title>
siteTitle: <site_title>
description: <description>
locale: en-GB
keywords: [<keyword1>, <keyword2>, <keyword3>]
url: /
siteUrl: <github_pages_url>

# Social
github: <github_username>
twitter: <twitter_username>
linkedin: <linkedin_username>
show_atom_feed: 'true'

# Theme
avatar: avatar_200.png # filename to use
thumbnail: avatar_100.png # filename to use

# "Hi, I'm _______"
name: <name>
email: <email>

# Google Analytics key, leave blank to ignore
google_analytics_key:
```

Static assets reside in `assets/`. Images, such as the ones used for the avatar and the thumbnail, are stored in `assets/imgs/`.

In `fragments/`, we define HTML fragments to reuse across all blog's pages. When building the website, these fragments receive arguments, identified by `${<argument_name>}`, directly from Thera; an example is `sidebar.html`:

```html
<nav>
  <h1>Hi.</h1>
  <a href="/">
    <img src="/assets/imgs/${avatar}" id="logo" alt="Blog logo" />
  </a>
  <h2>I'm <a href="/">${name}</a>.</h2>
  <div id="bio">
    <p>💻 This is an example bio.</p>
  </div>
  <div id="social">
    Where to find me:
    <div id="stalker">
      <a title="${github} on Github" href="https://github.com/${github}">
        <i class="fa fa-github-square"></i>
      </a>

      <a title="${twitter} on Twitter" href="https://twitter.com/${twitter}">
        <i class="fa fa-twitter-square"></i>
      </a>

      <a
        title="${name} on LinkedIn"
        href="https://www.linkedin.com/in/${linkedin}"
      >
        <i class="fa fa-linkedin-square"></i>
      </a>
    </div>
  </div>
</nav>
```

As you can see, we use different arguments: `${avatar}`, `${name}`, `${github}`, `${twitter}` and `${linkedin}`. Upon building, Thera will replace each one of them with the corresponding data specified in `data.yml`.

In `posts/`, we store the posts' Markdown files. For the building process to work, each filename has to start with the date of the post, as it will be parsed to inject the date in the final HTML file. Each Markdown file has to contain a YAML header storing the title and the description of the post. An example is `2020-03-13-example3.md`:

```markdown
---
title: Example blog post 3
description: An example
---

# Example

A blog post example with a link to [Wikipedia](https://en.wikipedia.org/wiki/Main_Page).
```

CSS styling files reside in `private-assets/css`. All files are combined into a single `all.css` file used in the main Thera template, `templates/default.html`. This CSS file uses `cssAsset`, a Thera Function defined in `build.sc`, to read the other files and populate itself at build time:

```css
${cssAsset: base}
${cssAsset: skeleton}
${cssAsset: screen}
${cssAsset: layout}
${cssAsset: syntax}
${cssAsset: pygments}
```

The main Thera templates are in `templates/`. The template `post.html` takes the post's body as parameter and outputs it wrapped in HTML, with date and title:

```html
---
[body]
website: false
---

<p class="meta">
  ${date}
  <a href="/">
    <i class="home fa fa-home"></i>
  </a>
</p>

<h1 class="title">${title}</h1>
<div id="post">${body}</div>
```

Instead, `default.html` represents the default template, combining sidebar, footer and metadata information. It uses `htmlFragment`, a Thera Function defined in `build.sc`, to process a given HTML fragment. It also takes the body of the page as parameter and uses some arguments defined in `data/data.yml`, such as `${siteUrl}` or `${name}`:

```html
---
[body]
website: true
---

<!DOCTYPE html>
<!--[if lt IE 7]><html class="ie ie6" lang="en"> <![endif]-->
<!--[if IE 7]><html class="ie ie7" lang="en"> <![endif]-->
<!--[if IE 8]><html class="ie ie8" lang="en"> <![endif]-->
<!--[if (gte IE 9)|!(IE)]><!-->
<html lang="en">
  <!--<![endif]-->
  <head>
    ${htmlFragment: google-tag-manager-head} ${htmlFragment: meta}

    <link rel="canonical" href="${siteUrl}${url}" />

    <link
      href="//fonts.googleapis.com/css?family=Open+Sans:600,800"
      rel="stylesheet"
      type="text/css"
    />
    <link rel="shortcut icon" href="/favicon.png" />
    <link
      rel="alternate"
      type="application/atom+xml"
      title="${name}"
      href="${siteUrl}/atom.xml"
    />

    <link rel="stylesheet" href="/assets/all.css" />
    <link
      href="https://maxcdn.bootstrapcdn.com/font-awesome/4.4.0/css/font-awesome.min.css"
      rel="stylesheet"
      integrity="sha256-k2/8zcNbxVIh5mnQ52A0r3a6jAgMGxFJFE2707UxGCk= sha512-ZV9KawG2Legkwp3nAlxLIVFudTauWuBpC10uEafMHYL0Sarrz5A7G79kXh5+5+woxQ5HM559XX2UZjMJ36Wplg=="
      crossorigin="anonymous"
    />
  </head>
  <body>
    ${htmlFragment: google-tag-manager-body}

    <div class="container">
      <div class="four columns sidebar">${htmlFragment: sidebar}</div>

      <div class="eleven columns content">
        ${body}

        <div class="footer">${htmlFragment: footer}</div>
      </div>
    </div>
  </body>
</html>
```

Finally, `index.html` is the entry-point of the blog. It uses `foreach`, a predefined Thera Function, to display the list of posts:

```html
---
title: Blog Posts
---

<div id="home">
  <h2><i class="fa fa-bookmark"></i> Blog Posts</h2>
  <ul id="blog-posts" class="posts">
    ${foreach: $allPosts, ${post =>
    <li>
      <span>${post.date} &raquo;</span><a href="${post.url}">${post.title}</a>
    </li>
    }}
  </ul>
</div>
```

### Part 4: Building with Thera

We will now see how to build our blog with Thera. Three Ammonite scripts are used for the task: `build.sc`, `post.sc` and `util.sc`.

`post.sc` stores the case class representing a post, and the factory function to create it from a file path:

```scala
import $ivy.`com.akmetiuk::thera:0.2.0-M1` // download Thera from Ivy

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
      ValueHierarchy.yaml(header)("title").asStr.value // build a ValueHierarchy from the post's header
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
```

`util.sc`, instead, provides useful functions to process multiple Thera templates, run commands and write files:

```scala
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
  write.over(f, str, createFolders = true, truncate = false)

def pipeThera(tmls: Template*)(implicit ctx: ValueHierarchy): String =
  tmls.tail.foldLeft(tmls.head.mkValue) { (v, tml) =>
    tml.mkValue.asFunction(v :: Nil)
  }.asStr.value
```

Finally, `build.sc`, shown below, builds the blog in the following way:

1. It reads all posts stored in `src/posts`
2. It builds the default ValueHierarchy `defaultCtx` by reading `src/data/data.yml`
3. It builds an additional ValueHierarchy `htmlFragmentCtx` storing the `htmlFragment` function
4. It processes the post and default Thera templates
5. It starts the actual build procedure by copying static assets to `_site/`, the compiled website directory
6. It processes CSS assets from `src/private-assets/css/`, reading them with the `cssAsset` function and combining them in `_site/assets/all.css` through a Thera template
7. It processes the posts, and stores the corresponding HTML in `_site/posts/`: it does so by generating the posts' HTML body, which is then passed as argument to the post template, which is in turn passed as argument to the default template
8. It generates `index.html` by reading the corresponding file, piping it into the default template and copying the result to `_site/index.html`
9. It removes from `_site/` any remaining code

```scala
import $ivy.`com.akmetiuk::thera:0.2.0-M1`

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
```

## Final remarks

Everything is now ready to deploy your blog! All you have to do left is:

1. Running `environment/deploy-docker-image.sh` to publish the Docker image
2. Updating `.github/workflows/ci.yaml` with the most recent version of the Docker image
3. Committing and pushing your code to GitHub

Once done, GitHub Actions will build and publish the blog to GitHub Pages in minutes.
