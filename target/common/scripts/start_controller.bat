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
title THOSE Controller (Local)

rem Save the current working directory
set savedir=%CD%

rem Change to the directory this batch file is executing from
cd %~dp0

rem Execute the application
cd ..
echo Please be patient, startup may take a while depending on the hardware platform.
java -jar bin\felix.jar

rem Restore the original working directory
cd %savedir%
set savedir=
