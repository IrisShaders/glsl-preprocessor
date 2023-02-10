# GLSL Preprocessing with the C Preprocessor in Java - based on JCPP

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.douira/glsl-preprocessor/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.douira/glsl-preprocessor)
[![javadoc](https://javadoc.io/badge2/io.github.douira/glsl-preprocessor/javadoc.svg)](https://javadoc.io/doc/io.github.douira/glsl-preprocessor)
[![Gradle Build](https://github.com/douira/glsl-preprocessor/actions/workflows/gradle.yml/badge.svg)](https://github.com/douira/glsl-preprocessor/actions/workflows/gradle.yml)

This is a fork of JCPP modified for preprocessing GLSL, specifically in [Iris](https://github.com/IrisShaders/Iris/).

## License

Licensed under [GPLv3](LICENSE) with [an exception](LICENSE.EXCEPTION) allowing the creation of derivative works in combination with LGPL code or linking with Minecraft. This means you are only allowed to distribute derivative works, including but not limited to software that uses glsl-preprocessor through Mixin, uses its API without distributing a compiled version of glsl-preprocessor, or extends or uses it in some other way, if they are licensed with a compatible license (for example LGPL or GPL with exception). You are required to give attribution and provide the source code, which may be done by linking to this repository. See the license files for details, custom licenses are available if necessary.

This licensing structure has been adapted from [glsl-transformer](https://github.com/IrisShaders/glsl-transformer/blob/main/README.md).

The original work (JCPP) is licensed under the Apache License 2.0 and the corresponding file headers have been retained with the addition of a notice of modification. This derivative work is licensed under its own different terms as described above.

## Introduction

The following section is from the original readme:

> The C Preprocessor is an interesting standard. It appears to be
> derived from the de-facto behaviour of the first preprocessors, and
> has evolved over the years. Implementation is therefore difficult.

This modified and update version of JCPP is meant for the preprocessing of GLSL source code that also uses C preprocessor macros but doesn't need any of the library loading logic. GLSL code needs to be preprocessed so that it can then be parsed and transformed with [glsl-transformer](https://github.com/IrisShaders/glsl-transformer) before being sent to the graphics card driver.

### Modifications

The modifications made to JCPP in this version of it are extensive; some are specific to preprocessing GLSL, some are more general changes of style and practice.

- Removed checked exceptions
- Reformatted much of the code
- Moved file system-related things into the `fs` package
- Added virtual file system functionality and improved string-handling aspects in `Preprocessor`
- Removed file system reading classes. The only file system reading functionality is now `ResourceFileSystem` that is used for testing
- Added virtual and in-memory file system classes and functionality
- Added support for GLSL-specific directives like `#extension`, `#version`, `#custom`, and a different type of `#line` that only uses numbers
- Added snapshot and error tests for GLSL-specific situations and functionality
- Removed guava and replaced it with builtin methods from the standard library
- Upgraded the source compatibility to Java 17 (LTS)
- Removed dead code and cleaned up some things

## Upcoming work

Planned/possible upcoming work:

- Cleanup of the existing test files and procedures
- Tests for `MemoryFileSystem` and `VirtualFileSystem`
- Tests for `toWhitespace`, `expr_char`, `expr_token`, #error directives, `addMacro`, `addWarning` in `Preprocessor`
- Tests for line continuation features in `MacroTokenSource`
