#!/usr/bin/env bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

docker build --no-cache -t \
  --file "$SCRIPT_DIR/Dockerfile" \
  gondolav/thera-blog:$(date +%F) .
docker login
#docker tag 440f15e3e6a3 gondolav/thera-blog:$(date +%F)
docker push gondolav/thera-blog:$(date +%F)
