cmake_minimum_required(VERSION 3.16)
project(jni_helper)

set(CMAKE_CXX_STANDARD 14)

# set compiler level
if (NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
    message(STATUS "Setting build type to 'RelWithDebInfo' as none was specified.")
    #不管CACHE里有没有设置过CMAKE_BUILD_TYPE这个变量，都强制赋值这个值为RelWithDebInfo
    set(CMAKE_BUILD_TYPE RelWithDebInfo CACHE STRING "Choose the type of build." FORCE)

    # 当使用cmake-gui的时候，设置构建级别的四个可选项
    set_property(CACHE CMAKE_BUILD_TYPE PROPERTY STRINGS "Debug" "Release"
            "MinSizeRel" "RelWithDebInfo")
endif ()

# set output directory
message(STATUS "project source dir: ${PROJECT_SOURCE_DIR}")
message(STATUS "bin dir: ${CMAKE_BINARY_DIR}")

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_DEBUG ${CMAKE_BINARY_DIR}/debug/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_DEBUG ${CMAKE_BINARY_DIR}/debug/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_DEBUG ${CMAKE_BINARY_DIR}/debug/bin)

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR}/release/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR}/release/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR}/release/bin)

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELWITHDEBINFO ${CMAKE_BINARY_DIR}/release/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELWITHDEBINFO ${CMAKE_BINARY_DIR}/release/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELWITHDEBINFO ${CMAKE_BINARY_DIR}/release/bin)

# set java dependency
include(UseJava)
if (DEFINED ENV{JAVA_HOME})
    set(JAVA_HOME "$ENV{JAVA_HOME}")
else ()
    set(JAVA_HOME /Users/qiupengtao/Develop/jdk/hotspot/jdk8u292-b10/Contents/Home)
endif ()
message(STATUS "JAVA_HOME  variable is defined or set as '${JAVA_HOME}'")

if (APPLE)
    message(STATUS "os: MacOS")
    set(JNI_INCLUDE_DIRS ${JAVA_HOME}/include ${JAVA_HOME}/include/darwin)
    set(JNI_LIB_DIRS ${JAVA_HOME}/jre/lib)
    add_compile_options(-fPIC)
elseif (WIN32)
    message(STATUS "os: Windows")
    set(JNI_INCLUDE_DIRS ${JAVA_HOME}/include ${JAVA_HOME}/include/win32 ${JAVA_HOME}/include/win32/bridge)
    set(JNI_LIB_DIRS ${JAVA_HOME}/jre/lib)
elseif (UNIX)
    message(STATUS "os: Unix like e.g. linux, bsd.")
    set(JNI_INCLUDE_DIRS ${JAVA_HOME}/include ${JAVA_HOME}/include/linux)
    set(JNI_LIB_DIRS ${JAVA_HOME}/jre/lib)
    add_compile_options(-fPIC)
endif ()

message(STATUS "JNI include dirs: ${JNI_INCLUDE_DIRS}")
include_directories(${JNI_INCLUDE_DIRS})
link_directories(${JNI_LIB_DIRS})

# set jni helper sources
set(JNI_HELPER_SOURCES JniHelper.hpp JniHelper.cpp)
add_library(jni_helper_static STATIC ${JNI_HELPER_SOURCES})
add_library(jni_helper SHARED ${JNI_HELPER_SOURCES})

# set jni example sources
set(JNI_HELPER_EXAMPLE_SOURCES
        example/com_fireflysource_jni_example_JniExample.h
        example/JniExample.cpp)
add_library(jni_helper_example SHARED ${JNI_HELPER_EXAMPLE_SOURCES})
target_link_libraries(jni_helper_example jni_helper_static)