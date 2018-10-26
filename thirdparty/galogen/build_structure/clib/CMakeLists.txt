cmake_minimum_required(VERSION 3.10)
project(galogen)

set(MAKE_GENERATOR_PLATFORM x64)
set(CMAKE_C_FLAGS -m64)
set(CMAKE_CXX_FLAGS -m64)

option(POSITION_INDEPENDENT_LIB "Use position independent code for static library (if applicable)" ON)

add_library(galogen_static STATIC gl.h gl.c)
set_target_properties(galogen_static PROPERTIES
        OUTPUT_NAME galogen
        POSITION_INDEPENDENT_CODE ${POSITION_INDEPENDENT_LIB})

add_library(galogen_shared SHARED gl.h gl.c)
set_target_properties(galogen_shared PROPERTIES
        OUTPUT_NAME galogen
        SOVERSION "1.0"
        VERSION "1.0")

