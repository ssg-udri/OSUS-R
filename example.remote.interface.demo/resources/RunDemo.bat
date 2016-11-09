rem ==============================================================================
rem  This software is part of the Open Standard for Unattended Sensors (OSUS)
rem  reference implementation (OSUS-R).
rem
rem  To the extent possible under law, the author(s) have dedicated all copyright
rem  and related and neighboring rights to this software to the public domain
rem  worldwide. This software is distributed without any warranty.
rem
rem  You should have received a copy of the CC0 Public Domain Dedication along
rem  with this software. If not, see
rem  <http://creativecommons.org/publicdomain/zero/1.0/>.
rem ==============================================================================

@echo off

rem Save the current working directory
set savedir=%CD%

rem Change to the directory this batch file is executing from
cd %~dp0

rem set to the location of the "mil.dod.th.ose.core.api" and the "com.google.protobuf" jars
set TH_LIBS="C:\THOSE libraries"

rem installation directory of the JDK 1.8
set JAVA_HOME="C:\Program Files (x86)\Java\jdk1.8.0_60"

rem location of the JDK command line tools
set JAVA_BIN=%JAVA_HOME%\bin

rem add the two jars and the source directory to the classpath
set LIBRARIES=%TH_LIBS%\mil.dod.th.ose.core.api.jar;%TH_LIBS%\com.google.protobuf.jar;%~dp0

rem compile the file with 'javac' and run it

@echo on
%JAVA_BIN%\javac -cp %LIBRARIES% terra\harvest\standalone\demo\RemoteInterfaceStandaloneDemo.java
%JAVA_BIN%\java -cp %LIBRARIES% terra.harvest.standalone.demo.RemoteInterfaceStandaloneDemo

rem Restore the original working directory
cd %savedir%
set savedir=
