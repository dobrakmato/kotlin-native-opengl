
kotlin-native-opengl
--------------
Renderer / game engine in Kotlin/Native with use of some native libraries
written currently only for OpenGL backend. Maybe it will support Vulkan
sometime in the future. 


## Building

### Automatically

You can run `build_all.sh` script to compile all third-party projects and then compile the
Kotlin one. This script is experimental but tested on [MSYS2 for Windows](https://github.com/orlp/dev-on-windows/wiki/Installing-GCC--&-MSYS2-). Building on other
platforms is not tested, however in theory should work.

You need to have working environment with: gcc, g++, make, cmake, sh, bash, java (for Gradle).

This will take around 5 - 6 minutes.

### Manually

1. Build all native projects in `thirdparty` folder using cmake to `cmake-build-debug` directories.
2. Build the main Kotlin project with gradle by the `build` task. Get a coffee ☕, this will take some time.

## Packages

- math
- io
- bf
- utils
- renderer
- engine

## Tools

- bfinfo - inspect and view various bf files
- imgconv - image importer (mipmap generator, dxt compressor)
- geoconv - geometry importer (lod generator, optimizer)
- matcomp - material compiler
