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

Static assets reside in `assets/`. Images, such as the ones used for the avatar and the thumbnail, are stored in `assets/imgs`.

In `fragments/`, we define HTML fragments to reuse across all blog's pages. When building the website, these fragments receive arguments, identified by "\${<argument_name>}", directly from Thera; an example is `sidebar.html`:

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

As you can see, we use different arguments: "${avatar}", "${name}", "${github}", "${twitter}" and "\${linkedin}". Upon building, Thera will replace each one of them with the corresponding data specified in `data.yml`.

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
$ {
  cssasset: base;
}
$ {
  cssasset: skeleton;
}
$ {
  cssasset: screen;
}
$ {
  cssasset: layout;
}
$ {
  cssasset: syntax;
}
$ {
  cssasset: pygments;
}
```

The main Thera templates are in `templates`. The template `post.html`, for example, takes the post's body as parameter and outputs it wrapped in HTML, with date and title:

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

Instead, `default.html` represents the default template, combining sidebar, footer and metadata information. It uses `htmlFragment`, a Thera Function defined in `build.sc`, to process a given HTML fragment. It also takes the body of the page as parameter and uses some arguments defined in `data/data.yml`, such as "${siteUrl}" or "${name}":

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

## Final remarks
