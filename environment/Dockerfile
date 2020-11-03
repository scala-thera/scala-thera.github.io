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
