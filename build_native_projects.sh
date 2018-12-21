#!/usr/bin/env bash

START_TIME=$SECONDS

# Script requires working environment with following tools:
# gcc, g++, make, cmake, sh, bash, java.
#
# This environment should be already working on developer Linux & OS X
# operating systems.
#
# This environment can be easily obtained with MSYS2 on Windows.
# For more information follow the guide: https://github.com/orlp/dev-on-windows/wiki/Installing-GCC--&-MSYS2-

echo "----------------------------------------------------------------"
echo "----------------------- KOTGIN BUILD SCRIPT --------------------"
echo "----------------------------------------------------------------"
echo "JAVA_HOME=$JAVA_HOME"

# Add JAVA_HOME to PATH in case it is not there.
export PATH="$JAVA_HOME/bin":$PATH
export PATH=/c/msys64/mingw32/bin:$PATH
export PATH=/c/msys64/mingw64/bin:$PATH
export PATH=$NATIVE_TOOLCHAIN:$PATH
echo "PATH=$PATH"

# create TEMP dir if not set for gcc
if [[ -z "${TMP}" ]]; then
    mkdir -p "buildtools/tmp"
    cd "buildtools/tmp"
    BUILDTOOLS_TMP=`pwd` # relative paths do not work on msys on windows
    cd ../../
    export TMP="$BUILDTOOLS_TMP"
    export TEMP="$BUILDTOOLS_TMP"
fi

CMAKE_GENERATOR="CodeBlocks - MinGW Makefiles"

SCRIPT_ARG1=$1

function color_msg {
    echo -e "\e[1;36m$1\e[0m"
}

echo "----------------------- Configuring build ----------------------"

# Checks the presence of tools specified by name by running a command
# and comparing its exit code with zero.
function check_tool_working {
    NAME=$1
    COMMAND=$2

    color_msg "--> Checking $1..."
    eval "$2"
    if [[ $? != 0 ]]; then echo "ERROR: $1 not working!"; exit 1; fi
}

check_tool_working "Gradle" "./gradlew -v"
check_tool_working "gcc" "gcc -v"
check_tool_working "g++" "g++ -v"
check_tool_working "cmake" "cmake --version"

echo "----------------- Building thirdparty projects -----------------"

function build_native_project {
    NAME=$1
    BIN_DIRECTORY=$2
    SRC_DIRECTORY=$3
    ADDITIONAL_CMAKE_ARGUMENTS=$4

    OLD_PWD=`pwd`
    color_msg "--> Building $NAME..."
    if [ "$SCRIPT_ARG1" == "clean" ]; then rm -rf "$BIN_DIRECTORY"; fi
    mkdir -p "$BIN_DIRECTORY"
    cd "$BIN_DIRECTORY"
    echo " - Configuring..."
    cmake -G"${CMAKE_GENERATOR}" -DCMAKE_BUILD_TYPE=Debug ${ADDITIONAL_CMAKE_ARGUMENTS} -DCMAKE_SH="CMAKE_SH-NOTFOUND" "${SRC_DIRECTORY}"
    if [[ $? != 0 ]]; then echo "ERROR: cannot run CMake for third party project '$NAME' !"; exit 1; fi
    if [ "$SCRIPT_ARG1" == "clean" ]; then cmake --build "." --target clean; fi
    echo " - Building..."
    cmake --build "." -- -j 4 # triggering build with cmake prevent som very strange bugs in windows
    if [[ $? != 0 ]]; then echo "ERROR: cannot build third party project $NAME!"; exit 1; fi
    cd "$OLD_PWD"
    echo "----------------------------------------------------------------"
}

function generate_opengl_bindings {
    color_msg "--> Building OpenGL binding generator (Galogen)..."
    OLD_PWD=`pwd`
    cd "thirdparty/galogen/"
    mkdir -p "build/clib"
    mkdir -p "build/galogen"
    cp "build_structure/clib/CMakeLists.txt" "build/clib/CMakeLists.txt"
    g++ galogen.cpp third_party/tinyxml2.cpp -static -o build/galogen/galogen.exe
    cd "build/galogen"
    color_msg "--> Generating OpenGL bindings..."
    ./galogen "../../third_party/gl.xml" --api gl --ver 4.5 --profile core
    if [[ $? != 0 ]]; then echo "ERROR: cannot generate OpenGL bindings using Galogen generator!"; exit 1; fi
    mv gl.* "../clib"
    cd "$OLD_PWD"
    echo "----------------------------------------------------------------"
}

CMAKE_BUILD_DIR="cmake-build-debug"

build_native_project "glfw" "thirdparty/glfw/$CMAKE_BUILD_DIR" "../"

# for some reason static libraries are not built if we do not pass the argument BUILD_STATIC_LIBS
build_native_project "lz4" "thirdparty/lz4/contrib/cmake_unofficial/$CMAKE_BUILD_DIR" "../" "-DBUILD_STATIC_LIBS=true"
build_native_project "stb" "thirdparty/stb/$CMAKE_BUILD_DIR" "../"

# to build OpenGL bindings first we need to generate them
generate_opengl_bindings
build_native_project "OpenGL bindings" "thirdparty/galogen/build/clib/$CMAKE_BUILD_DIR" "../"

ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo "----------------------------------------------------------------"
echo "Build script finished execution in $(($ELAPSED_TIME/60)) min $(($ELAPSED_TIME%60)) sec!"
echo "----------------------------------------------------------------"