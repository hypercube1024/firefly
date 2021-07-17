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

echo "$(uname)"

if [ "$(uname)" == "Darwin" ];then
  echo "build on MacOS"
  cmake -S "$PROJECT_HOME" -B "$RELEASE_BUILD_DIR"
  cmake --build "$RELEASE_BUILD_DIR" --target clean
  cmake --build  "$RELEASE_BUILD_DIR" --target all
  cd "$RELEASE_BUILD_DIR" && make
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ];then
  echo "build on Linux"
  cmake -S "$PROJECT_HOME" -B "$RELEASE_BUILD_DIR"
  cmake --build "$RELEASE_BUILD_DIR" --target clean
  cmake --build  "$RELEASE_BUILD_DIR" --target all
  cd "$RELEASE_BUILD_DIR" && make
elif [[ "$(expr substr $(uname -s) 1 10)" == "MINGW32_NT" || "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]];then
  echo "build on Windows"
  cmake -S "$PROJECT_HOME" -B "$RELEASE_BUILD_DIR"
  cmake --build "$RELEASE_BUILD_DIR" --target clean
  cmake --build  "$RELEASE_BUILD_DIR" --target ALL_BUILD
  cd "$RELEASE_BUILD_DIR" && msbuild.exe ALL_BUILD.vcxproj -t:rebuild -p:Configuration=Release
fi