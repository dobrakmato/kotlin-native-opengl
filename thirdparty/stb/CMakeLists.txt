cmake_minimum_required(VERSION 3.10)
project(stb C)

set(MAKE_GENERATOR_PLATFORM x64)
set(CMAKE_C_FLAGS -m64)
set(CMAKE_CXX_FLAGS -m64)

option(POSITION_INDEPENDENT_LIB "Use position independent code for static library (if applicable)" ON)
set(SOURCE_FILES stb_bindings.c)

add_library(stb_static STATIC ${SOURCE_FILES})
set_target_properties(stb_static PROPERTIES
        OUTPUT_NAME stb
        POSITION_INDEPENDENT_CODE ${POSITION_INDEPENDENT_LIB})

