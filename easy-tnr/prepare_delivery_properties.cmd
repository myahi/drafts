@echo off

setlocal enableextensions enabledelayedexpansion

echo.
echo ---------------------------------------------------------------------------
echo                      PREPARE EasyTNR DELIVERY VARIABLES
echo ---------------------------------------------------------------------------
echo.

set "ENV_NAME=%~1"
set "PROPERTIES_FILE_NAME=delivery.properties"

if not defined WORKSPACE (
	set WORKSPACE=%CD%
)

set "SERVER=sgl4shn04"
for /f %%i in ('wmic computersystem get domain ^| findstr /r "\."') do set "DOMAIN=%%i"
if "%DOMAIN%" == "dct.adt.local" (
	set "SERVER=%SERVER%.marches.intra.laposte.fr"
)

set "PACKAGE_ZIP=\\%SERVER%\CALYPSO_PACKAGES\%ENV_NAME%\%ENV_NAME%_all.zip"
echo PACKAGE_ZIP=%PACKAGE_ZIP%
if not exist "%PACKAGE_ZIP%" (
	echo Package zip file doesn't exist : %PACKAGE_ZIP%
	exit 1
)

set "CLIENT_ZIP=%WORKSPACE%\client.zip"
unzip -p %PACKAGE_ZIP% client.zip > %CLIENT_ZIP%

if not defined CALYPSO_VERSION (
	for /f "tokens=6 delims=-" %%i in ('unzip -l %CLIENT_ZIP% ^| findstr /R custom-shared-lib-[a-zA-Z0-9\.]*-SNAPSHOT\.jar') do (
		set "CALYPSO_VERSION=%%i-SNAPSHOT"
	)
)

if not defined CALYPSO_VERSION (
	for /f "tokens=6,7 delims=-" %%i in ('unzip -l %CLIENT_ZIP% ^| findstr /R custom-shared-lib-[a-zA-Z0-9\.]*-.*-SNAPSHOT\.jar') do (
		set "CALYPSO_VERSION=%%i-%%j-SNAPSHOT"
	)
)

if not defined CALYPSO_VERSION (
	for /f "tokens=6 delims=-" %%i in ('unzip -l %CLIENT_ZIP% ^| findstr /R custom-shared-lib-[a-zA-Z0-9\.]*\.jar') do (
		set "CALYPSO_VERSION=%%i"
		set "CALYPSO_VERSION=!CALYPSO_VERSION:.jar=!"
	)
)

if not defined CALYPSO_VERSION (
	for /f "tokens=6 delims=-" %%i in ('unzip -l %CLIENT_ZIP% ^| findstr /R custom-shared-lib-[a-zA-Z0-9\.]*-.*\.jar') do (
		set "CALYPSO_VERSION=%%i"
	)
)

if defined CALYPSO_VERSION (
	set CALYPSO_VERSION=%CALYPSO_VERSION:.jar=%
)

if "%CALYPSO_VERSION%"=="" (
	echo Could not determine CALYPSO_VERSION property
	echo.
	echo ERROR=1
	exit /B 1
) else (
	echo CALYPSO_VERSION=%CALYPSO_VERSION%
	echo CALYPSO_VERSION=%CALYPSO_VERSION%> %PROPERTIES_FILE_NAME%
	echo.
	exit /B 0
)
