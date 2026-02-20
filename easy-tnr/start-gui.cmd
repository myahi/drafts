@echo off
setlocal enableextensions enabledelayedexpansion

set "ENV_NAME=%~1"
if "%ENV_NAME%" == "" (
	echo ENV_NAME parameter is missing
	pause
	exit 1
)

set "SCRIPTS_PATH=%~dp0"

set "SERVER=sin1shr03"
for /f %%i in ('wmic computersystem get domain ^| findstr /r "\."') do set "DOMAIN=%%i"
if "%DOMAIN%" == "dct.adt.local" (
	set "SERVER=%SERVER%.marches.intra.laposte.fr"
)

set "JAVA_EXE=\\%SERVER%\CALYPSO_MOE\EasyTNR\java-1.8.0-openjdk-1.8.0.201-2.b09.redhat.windows.x86_64\bin\javaw.exe"
set "ENV_DIR=%TMP%\EasyTNR\%ENV_NAME%"
set "CONFIG_FILE=%ENV_DIR%\config\%ENV_NAME%.tnrConfigFile.properties"
if not exist %CONFIG_FILE% (
	echo Config file does not exist : %CONFIG_FILE%
	pause
	exit 1
)
set "LIBS_PATH=."
call %SCRIPTS_PATH%set_classpath.cmd

pushd %ENV_DIR%
echo Starting Easy TNR, please wait...
echo Config file : %CONFIG_FILE%
start %JAVA_EXE% -splash:".\resources\img\splashScreen.gif" -Duser.home="%HOMESHARE%" -Dtnr.configFile=%CONFIG_FILE% -Xmx512m lbp.qa.easy.tnr.gui.EasyTnrLauncherGUI
popd

endlocal
