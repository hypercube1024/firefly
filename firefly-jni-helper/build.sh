#!/bin/bash
PROJECT_HOME=$(cd "$(dirname "$0")";pwd)
echo "project dir: $PROJECT_HOME"

JNI_HELPER_SOURCES_DIR="$PROJECT_HOME/src/main/cpp/jni-helper"
echo "JNI helper sources dir: $JNI_HELPER_SOURCES_DIR"

JNI_HELPER_EXAMPLE_LIB_DIR="$JNI_HELPER_SOURCES_DIR/build-release/release/lib"
echo "JNI helper example lib dir: $JNI_HELPER_EXAMPLE_LIB_DIR"

JNI_HELPER_TEST_LIB_MACOS_DIR="$PROJECT_HOME/src/test/resources/lib/macos"

cd "$JNI_HELPER_SOURCES_DIR" && sh ./build.sh
cp "$JNI_HELPER_EXAMPLE_LIB_DIR/libjni_helper_example.dylib" "$JNI_HELPER_TEST_LIB_MACOS_DIR"
cd "$PROJECT_HOME" && mvn test


