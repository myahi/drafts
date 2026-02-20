@echo off
setlocal enableextensions enabledelayedexpansion

set "ENV_NAME=%1"
if "%ENV_NAME%" == "" (
	echo ENV_NAME parameter is missing
	pause
	exit 1
)

set "SCRIPTS_PATH=%~dp0"

if not defined JAVA_HOME (
	set "JAVA_HOME=D:\Client_apps\java-1.8.0-openjdk-1.8.0.201-2.b09.redhat.windows.x86_64"
)
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "ENV_DIR=%TMP%\EasyTNR\%ENV_NAME%"
set "CONFIG_FILE=%ENV_DIR%\config\%ENV_NAME%.tnrConfigFile.properties"
if not exist %CONFIG_FILE% (
	echo Config file does not exists : %CONFIG_FILE%
	pause
	exit 1
)
set "LOGGC_DIR=%ENV_DIR%\logs"
if not exist "%LOGGC_DIR%" (
	mkdir %LOGGC_DIR%
)

set "LIBS_PATH=."
REM call %SCRIPTS_PATH%set_classpath.cmd
set CLASSPATH=%LIBS_PATH%\resources;%LIBS_PATH%\*;

set "VM_ARGS="
:loop
if NOT "%4" == "" (
   	if NOT "%5" == "" (
   		set "VM_ARGS=%VM_ARGS% %4=%5"
	)
)

if "%4" == "" (
	goto next
)
if "%5" == "" (
	goto next
)

shift /4
shift /4
goto loop

:next
pushd %ENV_DIR%
echo Starting Easy TNR, please wait batch ...
echo ENV_DIR=%ENV_DIR%
%JAVA_EXE% -Duser.home="%ENV_DIR%" %VM_ARGS% -Dtnr.configFile=%CONFIG_FILE% -Djava.util.Arrays.useLegacyMergeSort=true -Xmx8g -Xms2g -Xss1m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC -Dsun.rmi.transport.tcp.handshakeTimeout=1200000 -Dsun.rmi.dgc.client.gcInterval=3600000 -Xloggc:%LOGGC_DIR%\%BATCH_NAME%_LOGGC.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -verbose:gc lbp.qa.easy.tnr.batch.EasyTnrLauncherBatch -batchName %~2 -scenarios %~3
rem echo "ERROR LEVEL FROM START-BATCH =>" %ERRORLEVEL%
popd

endlocal
