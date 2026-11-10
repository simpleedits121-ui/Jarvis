cmake_minimum_required(VERSION 3.22.1)
project(aether LANGUAGES CXX)

add_library(aether SHARED aether_native.cpp)

find_library(log-lib log)
find_library(android-lib android)
find_library(vulkan-lib vulkan)

target_compile_features(aether PRIVATE cxx_std_17)
target_compile_options(aether PRIVATE -Wall -Wextra -Werror=return-type)
target_link_libraries(aether PRIVATE ${log-lib} ${android-lib} ${vulkan-lib})
