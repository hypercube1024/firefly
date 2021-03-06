//
// Created by qiupengtao on 2021/7/9.
//

#ifndef TEST_JNI_JNI_HELPER_JNIHELPER_HPP_
#define TEST_JNI_JNI_HELPER_JNIHELPER_HPP_
#include <string>
#include <jni.h>

namespace com {
namespace fireflysource {
namespace jni {

template<typename JniType>
class LocalReference {
 public:
  template<typename CppType>
  LocalReference(JNIEnv *env,
                 const CppType &cppParam,
                 JniType(*newJavaType)(JNIEnv *, const CppType &)) {
    this->env = env;
    this->javaObject = newJavaType(env, cppParam);
  }

  ~LocalReference() {
    env->DeleteLocalRef(javaObject);
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
