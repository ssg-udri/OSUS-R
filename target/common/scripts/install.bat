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
rem  This file is for installing the controller as a service. Once the controller-app
rem  is installed as a Windows Service the contents of the directory should not be moved 
rem  without running the uninstaller or uninstall.bat.
rem    
rem ==============================================================================
@echo off

rem Save the current working directory so it can be returned to after execution of the script
set savedir=%CD%

rem Change to the directory this batch file is executing from
cd %~dp0

rem start controller app batch file
set startBat=start_controller.bat

rem Create full path to the start controller batch.
set fullPath=%~dp0%startBat%

rem run the installer
for /f "delims= " %%a in ('wmic os get osarchitecture') do (
    if /i "%%a"=="64-bit" (set bin_dir=nssm-win64)
    if /i "%%a"=="32-bit" (set bin_dir=nssm-win32)
)

%bin_dir%\nssm install ThoseController "%fullPath%"

rem Restore the original working directory
cd %savedir%
set savedir=