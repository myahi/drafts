@echo off
setlocal enabledelayedexpansion

echo.
echo ---------------------------------------------------------------------------
echo                               DELIVER EasyTNR
echo ---------------------------------------------------------------------------
echo.

set "ENV_NAME=%~1"
if not defined ENV_NAME (
	echo ENV_NAME should be specified as parameter
	exit 1
)

if not defined WORKSPACE (
	set WORKSPACE=%CD%
)

set "PACKAGES_SERVER=sgl4shn04"
for /f %%i in ('wmic computersystem get domain ^| findstr /r "\."') do set "DOMAIN=%%i"
if "%DOMAIN%" == "dct.adt.local" (
	set "SERVER=%PACKAGES_SERVER%.marches.intra.laposte.fr"
)

set "PACKAGE_ZIP=\\%PACKAGES_SERVER%\CALYPSO_PACKAGES\%ENV_NAME%\%ENV_NAME%_all.zip"
echo PACKAGE_ZIP=%PACKAGE_ZIP%
if not exist "%PACKAGE_ZIP%" (
	echo Package zip file doesn't exist : %PACKAGE_ZIP%
	exit 1
)

set "CLIENT_ZIP=%WORKSPACE%\client.zip"
unzip -p %PACKAGE_ZIP% client.zip > %CLIENT_ZIP%
if exist "%WORKSPACE%\client" (
	call psfile -accepteula localhost %WORKSPACE%\client\ -c
	rd /S /Q "%WORKSPACE%\client"
	ping 1.1.1.1 -n 1 -w 5000 > nul
)
unzip -q %CLIENT_ZIP% -d %WORKSPACE%

set "DELIVERY_SERVER=sin1shr03"
for /f %%i in ('wmic computersystem get domain ^| findstr /r "\."') do set "DOMAIN=%%i"
if "%DOMAIN%" == "dct.adt.local" (
	set "DELIVERY_SERVER=%DELIVERY_SERVER%.marches.intra.laposte.fr"
)

set "DELIVERY_SERVER=\\%DELIVERY_SERVER%"
set "DELIVERY_SERVER_CLIENT_LOCAL_PATH=C:\Applications\CALYPSO_DEV\releases\EasyTNR"
set "TARGET_DIRECTORY=%DELIVERY_SERVER%\CALYPSO_MOE\EasyTNR\libs\%ENV_NAME%"

if exist "%TARGET_DIRECTORY%" (
	call psfile -accepteula %DELIVERY_SERVER% %DELIVERY_SERVER_CLIENT_LOCAL_PATH%\libs\%ENV_NAME%\ -c
	rd /S /Q "%TARGET_DIRECTORY%"
	ping 1.1.1.1 -n 1 -w 5000 > nul
)
mkdir "%TARGET_DIRECTORY%"
xcopy /S /Y target\work\dependencies\*.jar %TARGET_DIRECTORY%\
echo 1
echo \dependencies\> exclude.txt
xcopy /Y /EXCLUDE:exclude.txt target\calypso-lbp-testing-*.jar %TARGET_DIRECTORY%\
echo 2
echo calypsosystem.properties.%ENV_NAME%> exclude.txt
echo calypsosystem.properties.%ENV_NAME%_ReadOnly>> exclude.txt
echo 3
xcopy /S /Y %WORKSPACE%\client\generated-resources\com\calypso\resources\main\*.* %TARGET_DIRECTORY%\
xcopy /S /Y /EXCLUDE:exclude.txt %WORKSPACE%\client\resources\*.* %TARGET_DIRECTORY%\resources\
echo 4
xcopy /S /Y src\main\scripts\*.* %TARGET_DIRECTORY%\scripts\

xcopy /Y target\work\set_classpath.cmd %TARGET_DIRECTORY%\scripts\

(echo set CLASSPATH=%%LIBS_PATH%%\calypso-generated-resources.jar;^^& echo %%LIBS_PATH%%\calypso-lbp-testing-*.jar;^^) > %TARGET_DIRECTORY%\scripts\set_classpath.cmd_new
more +1 %TARGET_DIRECTORY%\scripts\set_classpath.cmd >> %TARGET_DIRECTORY%\scripts\set_classpath.cmd_new
move /y %TARGET_DIRECTORY%\scripts\set_classpath.cmd_new %TARGET_DIRECTORY%\scripts\set_classpath.cmd

mkdir %TARGET_DIRECTORY%\config
if exist src\main\config\%ENV_NAME%.tnrConfigFile.properties (
	xcopy /Y src\main\config\%ENV_NAME%.tnrConfigFile.properties %TARGET_DIRECTORY%\config\
) else (
	type src\main\config\default.tnrConfigFile.properties | src\main\scripts\repl "@CALYPSO_ENV@" "%ENV_NAME%" > "%TARGET_DIRECTORY%\config\%ENV_NAME%.tnrConfigFile.properties"
)
xcopy /Y src\test\resources\img\splashScreen.gif %TARGET_DIRECTORY%\resources\img\
echo 5
set "CURRENT_DATE=%date:~6,4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
set "CURRENT_DATE=%CURRENT_DATE: =0%"
echo %CURRENT_DATE%> %TARGET_DIRECTORY%\config\delivery_date.txt
