#!/bin/bash
PROJECT_HOME=$(cd "$(dirname "$0")" && pwd)
echo "project dir: $PROJECT_HOME"

JNI_HELPER_EXAMPLE_HEADERS_DIR="$PROJECT_HOME/target/headers"
echo "JNI helper example headers dir: $JNI_HELPER_EXAMPLE_HEADERS_DIR"

JNI_HELPER_SOURCES_DIR="$PROJECT_HOME/src/main/cpp/jni-helper"
echo "JNI helper sources dir: $JNI_HELPER_SOURCES_DIR"

JNI_HELPER_EXAMPLE_LIB_DIR="$JNI_HELPER_SOURCES_DIR/build-release/release/lib"
echo "JNI helper example lib dir: $JNI_HELPER_EXAMPLE_LIB_DIR"

JNI_HELPER_EXAMPLE_LIB_MACOS_DIR="$PROJECT_HOME/src/test/resources/lib/macos"

# generate JNI header
cd "$PROJECT_HOME" && mvn clean compile
cp "$JNI_HELPER_EXAMPLE_HEADERS_DIR/"*.h "$JNI_HELPER_SOURCES_DIR/example"
echo "-- copy JNI example headers is complete"

# build JNI example project
cd "$JNI_HELPER_SOURCES_DIR" && sh ./build.sh
cp "$JNI_HELPER_EXAMPLE_LIB_DIR/libjni_helper_example.dylib" "$JNI_HELPER_EXAMPLE_LIB_MACOS_DIR"
echo "-- build JNI example cpp project is complete"

# run JNI test cases
cd "$PROJECT_HOME" && mvn test


