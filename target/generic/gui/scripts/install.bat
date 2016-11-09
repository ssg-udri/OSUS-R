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

rem Save the current working directory so it can be returned to after execution of the script
set savedir=%CD%
set xmlFileName=ThoseWebServerService.xml
set tempFile=%xmlFileName%.tmp

rem Change to the directory this batch file is executing from
cd %~dp0

CALL asadmin.bat create-service --name ThoseWebServer those 

rem Change to the directory that contains the Glassfish server service xml.
cd ..
cd domains\those\bin

rem Verify Glassfish service xml was created.
if not exist %xmlFileName% (
    echo Xml file with given name does not exist.
    exit /B 2
)

if exist %tempFile% del /F /Q %tempFile%

rem Add quotes to the file path stored within the Glassfish service xml.
setlocal DisableDelayedExpansion
(
    for /F "usebackq delims=" %%G in (%xmlFileName%) do (
       set LINE=%%G
       setlocal EnableDelayedExpansion
       if not "!LINE!" == "!LINE:domains<=!" (
            set LINE=!LINE:domains=domains"!
            set LINE=!LINE:^<startargument^>=^<startargument^>"!
            set LINE=!LINE:^<stopargument^>=^<stopargument^>"!
        )
        >> %tempFile% echo !LINE! 
        endlocal
    )
)
endlocal
xcopy /Y %tempFile% %xmlFileName%
del /F /Q %tempFile%

rem Restore the original working directory
cd %savedir%
set savedir=
