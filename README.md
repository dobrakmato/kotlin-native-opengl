
kotlin-native-opengl
--------------
Renderer / game engine in Kotlin/Native with use of some native libraries
written currently only for OpenGL backend. Maybe it will support Vulkan
sometime in the future. 


## Building
Building is currently very complicated.

1. Build all projects in thirdparty folder using CMake.
2. Run generateInterops gradle task.
3. Build gradle project.