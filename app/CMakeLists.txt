# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# Opencv stuff
# OpenCV-android- sdk and AugmentedAndroids-worktitle must be in the same folder
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../OpenCV-android-sdk/sdk/native/jni/include)
add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java.so)

add_library( native-lib SHARED src/main/cpp/native-lib.cpp )
find_library( log-lib log )

target_link_libraries( native-lib  lib_opencv $\{log-lib})