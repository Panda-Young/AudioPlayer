cmake_minimum_required(VERSION 3.22.1)
project(AudioPlayerNative) 

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_BUILD_TYPE Debug)  # or Release

add_subdirectory(gain)
