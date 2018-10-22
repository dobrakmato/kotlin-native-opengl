@set PATH=c:/msys64/mingw64/bin;%PATH%
mkdir build/galogen
mkdir build/clib
g++ galogen.cpp third_party/tinyxml2.cpp -static -o build/galogen/galogen.exe
cd build/galogen/
galogen ../../third_party/gl.xml --api gl --ver 4.5 --profile core
mv gl.* ../clib
cd ../clib
gcc -c gl.c
cd ../..
