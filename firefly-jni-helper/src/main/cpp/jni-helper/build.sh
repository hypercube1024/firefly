#!/bin/bash
PROJECT_HOME=$(cd "$(dirname "$0")" && pwd)
echo "project dir: $PROJECT_HOME"
RELEASE_BUILD_DIR="$PROJECT_HOME/build-release"
echo "cmake release dir: $RELEASE_BUILD_DIR"

if [ ! -d "$RELEASE_BUILD_DIR" ]; then
  mkdir "$RELEASE_BUILD_DIR"
else
  rm -rf "$RELEASE_BUILD_DIR"
fi

if [ "$(uname)" == "Darwin" ];then
# Mac OS X 操作系统
  echo "build on MacOS"
  cmake -S "$PROJECT_HOME" -B "$RELEASE_BUILD_DIR"
  cmake --build "$RELEASE_BUILD_DIR" --target clean
  cmake --build  "$RELEASE_BUILD_DIR" --target all
  cd "$RELEASE_BUILD_DIR" && make
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ];then
  # GNU/Linux操作系统
  echo "build on Linux"
  cmake -S "$PROJECT_HOME" -B "$RELEASE_BUILD_DIR"
  cmake --build "$RELEASE_BUILD_DIR" --target clean
  cmake --build  "$RELEASE_BUILD_DIR" --target all
  cd "$RELEASE_BUILD_DIR" && make
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW32_NT" ];then
  # Windows NT操作系统
  echo "build on Windows"
  cmake -S "$PROJECT_HOME" -B "$RELEASE_BUILD_DIR"
  cmake --build "$RELEASE_BUILD_DIR" --target clean
  cmake --build  "$RELEASE_BUILD_DIR" --target ALL_BUILD
  cd "$RELEASE_BUILD_DIR" && msbuild.exe ALL_BUILD.vcxproj -t:rebuild -p:Configuration=Release
fi