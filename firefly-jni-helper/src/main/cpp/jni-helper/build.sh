#!/bin/bash
PROJECT_HOME=$(cd "$(dirname "$0")" && pwd)
echo "project dir: $PROJECT_HOME"
RELEASE_BUILD_DIR="$PROJECT_HOME/build-release"
echo "cmake release dir: $RELEASE_BUILD_DIR"

if [ ! -d "$RELEASE_BUILD_DIR" ]; then
  mkdir "$RELEASE_BUILD_DIR"
fi

cmake -S "$PROJECT_HOME" -B "$RELEASE_BUILD_DIR"
cmake --build "$RELEASE_BUILD_DIR" --target clean -- -j 4
cmake --build  "$RELEASE_BUILD_DIR" --target all -- -j 4

cd "$RELEASE_BUILD_DIR" && make