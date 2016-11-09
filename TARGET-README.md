<!--
==============================================================================
 This software is part of the Open Standard for Unattended Sensors (OSUS)
 reference implementation (OSUS-R).

 To the extent possible under law, the author(s) have dedicated all copyright
 and related and neighboring rights to this software to the public domain
 worldwide. This software is distributed without any warranty.

 You should have received a copy of the CC0 Public Domain Dedication along
 with this software. If not, see
 <http://creativecommons.org/publicdomain/zero/1.0/>.
==============================================================================
-->

[README.md](README.md) contains basic instructions on building the software. This contains additional information about 
building platform/target specific components for different architectures and/or Linux distributions.

# Toolchain
A cross compilation toolchain is required to build the native code (e.g. serial port plug-in) for a specific target
platform. The toolchain can be made available in the system PATH environment variable or a full path to the `gcc`
executable can be specified in property files described below.

Example of installing an ARM toolchain on Ubuntu:

    sudo apt-get install gcc-arm-linux-gnueabi g++-arm-linux-gnueabi

# Building
## Controller Application
Follow standard build configuration steps defined in [README.md](README.md).

Copy `target/linux/example.build.properties` to `target/linux/build.properties`

Edit `target/linux/build.properties` with desired configuration. If the `gcc` executable is not available on the system
PATH, full paths to the executables should be specified.

Run the Ant build for the Linux controller application. The custom build properties are also applied if running the Ant
build from the top level project directory.

    cd target/linux
    ant build-with-deps deploy

Linux controller zip file is produced at `target/linux/controller-app/bin/controller-app-linux-<c.arch>.zip`

## Standalone Serial Port
At a minimum, a generic or Linux controller application build must be executed before attempting to build the
`mil.dod.th.ose.linux.serial` project by itself.

Manually create or edit `cnf/ext/platform.bnd` to define the following properties:

    c.arch: arm_le
    c.options: -O3 -Wall -Wpointer-arith -Wcast-align -Wno-unused-function -fPIC
    exe.cc: arm-linux-gnueabi-gcc
    exe.ln: arm-linux-gnueabi-gcc
    exe.strip: arm-linux-gnueabi-strip

Build serial port projects in the following order:

    cd mil.dod.th.ose.linux.serial.swig
    ant build
    cd mil.dod.th.ose.linux.serial
    ant clean build

Platform specific plug-in is produced at
`mil.dod.th.ose.linux.serial/generated/mil.dod.th.ose.linux.serial.<c.arch>.jar`

