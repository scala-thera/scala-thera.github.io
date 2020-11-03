#!/usr/bin/env bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

docker build --no-cache -t \
  --file "$SCRIPT_DIR/Dockerfile" \
  thera/thera-blog.com:$(date +%F) .
docker login
docker push thera/thera-blog.com:$(date +%F)
