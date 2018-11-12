
kotlin-native-opengl
--------------
Renderer / game engine in Kotlin/Native with use of some native libraries
written currently only for OpenGL backend. Maybe it will support Vulkan
sometime in the future. 


## Building
Building is currently complicated.

1. Build all native projects in `thirdparty` folder using cmake to `cmake-build-debug` directories.
2. Build the main Kotlin project with gradle by the `build` task. Get a coffee ☕, this will take some time.