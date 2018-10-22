
kotlin-native-opengl
--------------
Renderer / game engine in Kotlin/Native with use of some native libraries
written currently only for OpenGL backend. Maybe it will support Vulkan
sometime in the future. 


## Building
Building is currently very complicated.

1. Build all native projects in `thirdparty` folder using cmake to `cmake-build-debug` directories.
2. Run `:generateInterops` gradle task.
3. Build gradle project.