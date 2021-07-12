//
// Created by qiupengtao on 2021/7/12.
//
#include "com_fireflysource_jni_example_JniExample.h"
#include "../JniHelper.hpp"

namespace com {
namespace fireflysource {
namespace jni {
namespace example {

std::string sayHello(const std::string &str) {
  return "Bonjour, " + str;
}

}
}
}
}

using namespace com::fireflysource::jni;

JNIEXPORT jstring JNICALL Java_com_fireflysource_jni_example_JniExample_sayHello
    (JNIEnv *env, jclass javaClass, jstring javaString) {
  if (javaString == nullptr) {
    return newJavaString(env, "param str cannot be null");
  }

  std::string param = javaStringToCppString(env, javaString);
  std::string hello = example::sayHello(param);  // Call native library
  java::println(env, "call java method to print result: " + hello);

  return newJavaString(env, hello);
}