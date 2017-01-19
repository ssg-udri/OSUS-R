@echo off
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
rem
rem DESCRIPTION:
rem Script to start the SDK command line tool in a Windows environment.
rem
rem==============================================================================

rem Save reference to the base directory of the SDK
set SDK_DIR=%~dp0

set CLASSPATH="%SDK_DIR%lib\mil.dod.th.ose.sdk.jar";"%SDK_DIR%api\mil.dod.th.core.api.jar";"%SDKDIR%lib\biz.aQute.bndlib-3.1.0.jar"

rem Execute the application
java -cp %CLASSPATH% mil.dod.th.ose.sdk.those.ThoseMain %*

set CLASSPATH=