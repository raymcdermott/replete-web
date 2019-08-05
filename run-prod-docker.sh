#!/usr/bin/env bash

set -e

## Clean
rm -rf resources/public/js/compiled
rm -rf .cpcache
rm -rf cljs-test-runner-out

## Build
clojure -A:dist

# Run Docker
export IMAGE_VERSION=$RANDOM 
docker build -t replete-$IMAGE_VERSION . 
docker run -p 8888:80 replete-$IMAGE_VERSION

