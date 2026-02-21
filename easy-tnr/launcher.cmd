@echo off
setlocal enableextensions enabledelayedexpansion

set "ENV_NAME=%~1"
if "%ENV_NAME%" == "" (
	echo ENV_NAME parameter is missing
	pause
	exit 1
)
set "SCRIPT_NAME=%~2"

set "SERVER_NAME=sin1shr03"
for /f %%i in ('wmic computersystem get domain ^| findstr /r "\."') do set DOMAIN=%%i
if "%DOMAIN%" == "dct.adt.local" (
	set "SERVER=%SERVER_NAME%.marches.intra.laposte.fr"
) else (
	set "SERVER=%SERVER_NAME%"
)

set "BASE_DIR=\\%SERVER%\CALYPSO_MOE\EasyTNR\libs\%ENV_NAME%"
set /p DELIVERY_DATE=<%BASE_DIR%\config\delivery_date.txt
set "BASE_LOCAL_DIR=c:\serveur_apps\easy-tnr\%ENV_NAME%"

if exist "%BASE_LOCAL_DIR%\config\delivery_date.txt" (
	set /p LOCAL_DELIVERY_DATE=<%BASE_LOCAL_DIR%\config\delivery_date.txt
)
if not "%LOCAL_DELIVERY_DATE%" == "%DELIVERY_DATE%" (
	robocopy /MIR /MT:32 %BASE_DIR% %BASE_LOCAL_DIR%
	if "%DOMAIN%" == "dct.adt.local" (
		type %BASE_DIR%\config\%ENV_NAME%.tnrConfigFile.properties | %BASE_DIR%\scripts\repl "\\%SERVER_NAME%\\" "\%SERVER%\" > "%BASE_LOCAL_DIR%\config\%ENV_NAME%.tnrConfigFile.properties"
	) else (
		type %BASE_DIR%\config\%ENV_NAME%.tnrConfigFile.properties | %BASE_DIR%\scripts\repl "\\%SERVER%\\" "\%SERVER_NAME%\" > "%BASE_LOCAL_DIR%\config\%ENV_NAME%.tnrConfigFile.properties"
	)
)

set "VM_ARGS="
:loop
if NOT "%~5" == "" (
	if NOT "%~6" == "" (
		set "VM_ARGS=%VM_ARGS% %~5=%~6"
	)
)

if "%~5" == "" (
	goto next
)
if "%~6" == "" (
	goto next
)

shift /5
shift /5
goto loop
:next

if not "%SCRIPT_NAME%" == "" (
	if not exist "%BASE_LOCAL_DIR%\scripts\%SCRIPT_NAME%.cmd" (
		echo Script to launch does not exist : %BASE_LOCAL_DIR%\scripts\%SCRIPT_NAME%.cmd
		pause
		exit 1
	)
	call "%BASE_LOCAL_DIR%\scripts\%SCRIPT_NAME%.cmd" %ENV_NAME% %~3 %~4 %VM_ARGS%
)

rem ping 1.1.1.1 -n 1 -w 5000 > nul
