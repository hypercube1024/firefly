//
// Created by qiupengtao on 2021/7/9.
//

#ifndef TEST_JNI_JNI_HELPER_JNIHELPER_HPP_
#define TEST_JNI_JNI_HELPER_JNIHELPER_HPP_
#include <string>
#include <jni.h>
#include <iostream>

namespace com {
namespace fireflysource {
namespace jni {

template<typename JniType>
class LocalReference {
 public:
  LocalReference(JNIEnv *env, JniType javaObject) {
    this->env = env;
    this->javaObject = javaObject;
  }

  ~LocalReference() {
    env->DeleteLocalRef(javaObject);
    std::cout << "delete local reference." << std::endl;
  };

  JniType get() {
    return javaObject;
  }

 private:
  JniType javaObject;
  JNIEnv *env = nullptr;
};

std::string javaStringToCppString(JNIEnv *env, jstring javaString);

jstring newJavaString(JNIEnv *env, const std::string &cppString);

LocalReference<jstring> newJavaStringLocalReference(JNIEnv *env, const std::string &cppString);

namespace java {

void println(JNIEnv *env, jstring javaString);

void println(JNIEnv *env, const std::string &cppString);

}

}
}
}

#endif //TEST_JNI_JNI_HELPER_JNIHELPER_HPP_
