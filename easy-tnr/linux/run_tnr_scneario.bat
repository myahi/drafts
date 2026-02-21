set CURRENT_DATETIME=%date:~6,4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%
set CURRENT_DATETIME=%CURRENT_DATETIME: =0%

echo.
echo JAVA_HOME=%JAVA_HOME%
echo ENV=%ENV%
echo BATCH_NAME=%BATCH_NAME%
echo SCENARIOS=%SCENARIOS%
set VM_PARAMS=%VM_PARAMS% -DcurrentDate=%CURRENT_DATETIME:~0,8% -DcurrentDateyyMMdd=%CURRENT_DATETIME:~2,6%
echo %VM_PARAMS%
echo.

call %EASY_TNR_DIRECTORY%\libs\%ENV%\scripts\launcher.cmd %ENV% start-batch %BATCH_NAME% %SCENARIOS% %VM_PARAMS%

if ERRORLEVEL 1 (
	set EXIT_CODE=1
) else (
	set EXIT_CODE=0
)
echo "ERROR LEVEL =>" %EXIT_CODE%

exit /B %EXIT_CODE%
