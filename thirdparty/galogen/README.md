Galogen
=======

Galogen generates code to load OpenGL entry points  for the exact API version,
profile and extensions that you specify. 
This is fork of the Galogen, tweaked to be used with Kotlin Native.
If you are looking for Galogen itself, visit original [repository](https://github.com/google/galogen) or [website](http://galogen.gpfault.net/).

Usage
=======

This fork includes build scripts for Windows, which generate bindings for OpenGL core profile version 4.5. You can change this manually in `compile.bat`.
It could probably work on other platforms too, but was tested only on Windows, and includes only Windows scripts.

* `build-klib.bat` will produce ready-to-use `.klib` in the out folder.
* `build-folder.bat` will generate folder with `.o`, `.h` and `.def` files, to-be compiled by Kotlin Native Gradle Plugin, or you can do it manually using cinterop tool.
**Requirements**:
You will need msys2 in order to use any of the build scripts. You will also need Kotlin Native cinterop tool available in your path in order to use `build-klib.bat`.
**Notes**:
Paths in `compile.bat` and `galogen.def` probably need to be adjusted for your PC.
Specifically, you need to change `c:\msys2` in `compile.bat` to your msys2 installation folder, and you will need to change `C:/galogen` in `galogen.def` to an actual place where you place this repository.
You can leave path in `headers` as relative (`build/clib/gl.h`), but only if you are using `build-folder.bat`.
Relative paths for header only seem to work in cinterop tool only when used by Gradle Kotlin Native plugin, and relative path for linkerOpts doesn't seem to work at all.
