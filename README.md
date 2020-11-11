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

### Part 4: Building with Thera

## Final remarks
