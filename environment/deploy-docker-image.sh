#!/usr/bin/env bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

docker build --no-cache \
  --file "$SCRIPT_DIR/Dockerfile" \
  -t gondolav/thera-blog:$(date +%F) .
docker login
docker push gondolav/thera-blog:$(date +%F)
