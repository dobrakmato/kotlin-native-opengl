
kotlin-native-opengl
--------------
![build status](https://ci.appveyor.com/api/projects/status/go9a25d0kyq6ir7i/branch/master?svg=true)

Renderer / game engine in Kotlin/Native with use of some native libraries.
Currently has only the OpenGL backend. Maybe it will support Vulkan
sometime in the future. 


## Building

### Automatically

Running the gradle tasks should be enough.
```
./gradlew buildNativeProjects generateInterops build
```

It will automaticaly compile all native projects and the the kotlin one.
This script is experimental but tested on [MSYS2 for Windows](https://github.com/orlp/dev-on-windows/wiki/Installing-GCC--&-MSYS2-). Building on other
platforms is not tested, however in theory should work.

You need to have working environment with: gcc, g++, cmake, bash, java (for Gradle).

This will take around 5 - 10 minutes.

### Manually

1. Build all native projects in `thirdparty` folder using cmake to `cmake-build-debug` directories.
2. Build the main Kotlin project with gradle by the `build` task. Get a coffee ☕, this will take some time.

## Packages

- math - vec2, vec3, mat4, quaternions, scalar utils
- io - text file reading, binary file reading, reading and writing, abs path, relative path, extension, join two paths, buff(readXY, writeXY), File.XYat(pos)
- utils - timer, cmdopts parsing, logging, primitive vectors (arraylists), env variables
- bf - bf headers parsing, bf data access, compression & decompression, loading to memory, bf image, bf geometry
- bf_tools - bfinfo, bfview, imgconv, geoconv, matmake
- engine - gl objects, asset pipeline, visibility, scene graph, rendering, input, windows

## Tools

- bfinfo - inspect and view various bf files
- bfview - image viewer for compiled bf files (images, materials, geometries)
- imgconv - image importer (mipmap generator, dxt compressor)
- geoconv - geometry importer (lod generator, optimizer)
- matcomp - material compiler
