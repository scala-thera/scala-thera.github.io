# thera-example

In this tutorial, you will build a blog powered by [Thera](https://github.com/scala-thera/thera) and published on [GitHub Pages](https://pages.github.com).

By using Thera, you will be able to reduce code duplication, increase your productivity and better organize your project.

You can already have a look at the final result [here](https://scala-thera.github.io). The source code of the tutorial is hosted on [GitHub](https://github.com/scala-thera/scala-thera.github.io).

## Prerequisites

To follow this tutorial, you need to:

1. Create a public GitHub repository; it will host the blog source code.
2. Create a [Docker](https://www.docker.com) repository; it will host the image used by [GitHub Actions](https://github.com/features/actions) to publish the website.

## Structure

We will use [Ammonite](https://ammonite.io) and [os-lib](https://github.com/lihaoyi/os-lib) in conjunction with [Pandoc](https://pandoc.org) to setup the build procedure, with Thera streamlining the process. The procedure, driven by various Thera templates, will process assets, CSS files and posts (in Markdown) and generate the blog's HTML.

Here's the basic project structure:

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
