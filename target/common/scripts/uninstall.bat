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
set savedir="%CD%"

rem Change to the directory this batch file is executing from
cd %~dp0

SC QUERY ThoseController
IF ERRORLEVEL == 1060 GOTO END

rem Need to use net so the service is not marked for deletion, but rather removed
net stop ThoseController
sc delete ThoseController

:END
rem Restore the original working directory
cd %savedir%
set savedir=