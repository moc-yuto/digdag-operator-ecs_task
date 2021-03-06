#!/usr/bin/env bash

ROOT=$(cd $(dirname $0)/..; pwd)
EXAMPLE_ROOT=$ROOT/example
LOCAL_MAVEN_REPO=$ROOT/build/repo

CLUSTER="$1"
OUTPUT="$2"

if [ -z "$CLUSTER" ]; then
    echo "[ERROR] Set cluster as the first argument."
    exit 1
fi
if [ -z "$OUTPUT" ]; then
    echo "[ERROR] Set output s3 URI as the second argument."
    exit 1
fi

(
  cd $EXAMPLE_ROOT

  ## to remove cache
  rm -rfv .digdag

  ## run
  digdag run example.dig -c digdag.properties -p repos=${LOCAL_MAVEN_REPO} -p output=${OUTPUT} -p cluster=${CLUSTER}  --no-save
)
