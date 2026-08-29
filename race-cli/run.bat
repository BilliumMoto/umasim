@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "CRASH_LOG_DIR=%SCRIPT_DIR%build\crash-logs"

if not exist "%CRASH_LOG_DIR%" mkdir "%CRASH_LOG_DIR%"

java "-Xms16m" "-Xmx64m" "-XX:+UseSerialGC" "-XX:ReservedCodeCacheSize=32m" "-Xss512k" "-XX:ErrorFile=%CRASH_LOG_DIR%\hs_err_pid%%p.log" "-XX:ReplayDataFile=%CRASH_LOG_DIR%\replay_pid%%p.log" -jar "%SCRIPT_DIR%build\libs\race-cli-1.0-all.jar" %*
exit /b %ERRORLEVEL%
