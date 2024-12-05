set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)
cmake_minimum_required(VERSION 3.4.1)

# ncnn 라이브러리 경로 설정
set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

# OpenCV 라이브러리 경로 설정
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/opencv/${ANDROID_ABI}/sdk/native/jni)
find_package(OpenCV REQUIRED)

# 공유 라이브러리로 변경
add_library(nanodet SHARED
    nanodet.cpp
)

# 안드로이드 관련 라이브러리 추가
target_link_libraries(nanodet
    ncnn
    ${OpenCV_LIBS}
    jnigraphics
    android
    log
)