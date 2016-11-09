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

# PC SETUP
Building the THOSE software requires a Java 1.8 SDK, Ant 1.8.4 and Protocol Buffers 2.6.1. Ant and Protocol Buffers 
files are included in the `deps` directory. If building the Linux or EMAC platform interfaces (happens when running 
default build target), SWIG 3.x is required to support the use of JNI. If the data (video) streaming extensions will
be used, VLC 2.2.1 must be installed to support the transcoder service.

## Ant Build Properties
Default build properties defined in the `cnf/ant/default.properties` file be overridden using a build.properties file
in the top level directory of the project. Rename `example.build.properties` to `build.properties` and define any
needed properties.

## Environment Variable Setup (All Systems)
NOTE: The `setenv.sh.example` and `setenv.bat.example` files can be used as templates for setting environment variables.

1. Set `ANT_HOME` to the full path of the `deps/apache-ant-1.8.4` folder, e.g., 
   `ANT_HOME=C:\Projects\those\deps\apache-ant-1.8.4`.
2. Set `JAVA_HOME` to the full path of the installed JDK 8, e.g., `JAVA_HOME=C:\Program Files\Java\jdk1.8.0_60`.
3. If SWIG is being used, add the SWIG installation directory to the system PATH variable.

## Additional Environment Setup (Linux/OS X only)
The Protocol Buffer compiler binary `protoc` must be built for Linux/OS X. Follow the *C++ Installation - Unix* section 
of the README file located in `deps\protobuf-2.6.1` (this works for OS X too). A default configuartion should install 
the `protoc` binary to a system path. If a different location is selected, the location must either be on the system 
path or the environment variable `PROTOC_2_6_1` must be set that contains the path to the installation (see setenv.sh
script).

# BUILD CONFIGURATION
Building of THOSE can be customized by editing `build.properties` found in the same directory as this README (imported 
by all projects) or individual `build.properties` files for each project. An example property file is stored in the same
 directory as this README named `example.build.properties` to show which properties are typically customized.

# BUILDING FROM COMMAND LINE
Ant is used to build THOSE from the command line. The default target will build all projects, including projects that 
require SWIG. Deployments will be created for the controller, web GUI and SDK for a generic target and any other targets
that are supported by the host machine. Run the default target by executing `ant` from the command line at the top level
project directory.
     - Controller can be executed from target/generic/controller-app/deploy/bin (on local PC)
     - Controller install file (zipped) is located at target/generic/controller-app/bin
     - Web GUI can be executed from target/generic/gui/deploy/bin
     - Web GUI install file (zipped) is located at target/generic/gui/bin
     - SDK can be executed from target/generic/sdk-app/deploy/bin
     - SDK install file (zipped) is located at target/generic/sdk-app/bin

Individual deployments can be built by running the `build-deploy` Ant target for a specific target application folder. 
This only builds projects needed for the target. For example, to build the generic controller/GUI/SDK applications only:

    cd target/generic
    ant build-with-deps deploy

To build the Linux controller application only:

    cd target/linux
    ant build-with-deps deploy

If using Eclipse to build, follow these [instructions](ECLIPSE-README.md).

# BUILDING FOR DIFFERENT PLATFORMS
If building for a specific Linux platform, follow these [instructions](TARGET-README.md). By default, a full build
executed from the top level project directory (on a Linux PC) will generate a Linux controller application for for
the host PC (e.g. x86 or x86_64).

