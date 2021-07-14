#!/bin/bash
PROJECT_HOME=$(cd "$(dirname "$0")" && pwd)
echo "project dir: $PROJECT_HOME"

JNI_HELPER_EXAMPLE_HEADERS_DIR="$PROJECT_HOME/target/headers"
echo "JNI helper example headers dir: $JNI_HELPER_EXAMPLE_HEADERS_DIR"

JNI_HELPER_SOURCES_DIR="$PROJECT_HOME/src/main/cpp/jni-helper"
echo "JNI helper sources dir: $JNI_HELPER_SOURCES_DIR"


# generate JNI header
cd "$PROJECT_HOME" && mvn clean compile
cp "$JNI_HELPER_EXAMPLE_HEADERS_DIR/"*.h "$JNI_HELPER_SOURCES_DIR/example"
echo "-- copy JNI example headers is complete"

# build JNI example project
cd "$JNI_HELPER_SOURCES_DIR" && sh ./build.sh


JNI_HELPER_EXAMPLE_LIB_DIR="$JNI_HELPER_SOURCES_DIR/build-release/release"
echo "JNI helper example lib dir: $JNI_HELPER_EXAMPLE_LIB_DIR"

if [ "$(uname)" == "Darwin" ];then
  # Mac OS X 操作系统
  if [ -f "$JNI_HELPER_EXAMPLE_LIB_DIR/lib/libjni_helper_example.dylib" ]; then
    cp "$JNI_HELPER_EXAMPLE_LIB_DIR/lib/libjni_helper_example.dylib" "$PROJECT_HOME/src/test/resources/lib/macos"
  fi
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ];then
  # GNU/Linux操作系统
  if [ -f "$JNI_HELPER_EXAMPLE_LIB_DIR/lib/libjni_helper_example.so" ]; then
    cp "$JNI_HELPER_EXAMPLE_LIB_DIR/lib/libjni_helper_example.so" "$PROJECT_HOME/src/test/resources/lib/linux"
  fi
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW32_NT" ];then
  # Windows NT操作系统
  if [ -f "$JNI_HELPER_EXAMPLE_LIB_DIR/bin/jni_helper_example.dll" ]; then
    cp "$JNI_HELPER_EXAMPLE_LIB_DIR/bin/jni_helper_example.dll" "$PROJECT_HOME/src/test/resources/lib/windows/libjni_helper_example.dll"
  fi
fi

echo "-- build JNI example cpp project is complete"

# run JNI test cases
cd "$PROJECT_HOME" && mvn test


