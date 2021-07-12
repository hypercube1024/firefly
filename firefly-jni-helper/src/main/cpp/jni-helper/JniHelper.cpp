//
// Created by qiupengtao on 2021/7/9.
//
#include "JniHelper.hpp"

namespace com {
namespace fireflysource {
namespace jni {

std::string javaStringToCppString(JNIEnv *env, jstring javaString) {
  const char *cString = env->GetStringUTFChars(javaString, nullptr);
  std::string result = cString;
  env->ReleaseStringUTFChars(javaString, cString);
  return result;
}

jstring newJavaString(JNIEnv *env, const std::string &cppString) {
  auto result_buffer = static_cast<char *>(std::malloc(cppString.size()));
  std::strcpy(result_buffer, cppString.c_str());
  return env->NewStringUTF(result_buffer);
}

LocalReference<jstring> newJavaStringLocalReference(JNIEnv *env, const std::string &cppString) {
  return LocalReference<jstring>(env, cppString, newJavaString);
}

namespace java {

void println(JNIEnv *env, jstring javaString) {
  // Get system class
  jclass system = env->FindClass("java/lang/System");
  // Lookup the "out" field
  jfieldID fid = env->GetStaticFieldID(system, "out", "Ljava/io/PrintStream;");
  jobject out = env->GetStaticObjectField(system, fid);
  // Get PrintStream class
  jclass printStream = env->FindClass("java/io/PrintStream");
  // Lookup printLn(String)
  jmethodID printlnMethod = env->GetMethodID(printStream, "println", "(Ljava/lang/String;)V");
  env->CallVoidMethod(out, printlnMethod, javaString);
}

void println(JNIEnv *env, const std::string &str) {
  LocalReference<jstring> javaStringRef = newJavaStringLocalReference(env, str);
  jstring javaString = javaStringRef.get();
  println(env, javaString);
}

}

}
}
}