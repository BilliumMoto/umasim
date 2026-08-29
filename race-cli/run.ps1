$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$crashLogDir = Join-Path $scriptDir "build\crash-logs"
$jarPath = Join-Path $scriptDir "build\libs\race-cli-1.0-all.jar"

New-Item -ItemType Directory -Force -Path $crashLogDir | Out-Null

& java "-Xms16m" "-Xmx64m" "-XX:+UseSerialGC" "-XX:ReservedCodeCacheSize=32m" "-Xss512k" "-XX:ErrorFile=$crashLogDir\hs_err_pid%p.log" "-XX:ReplayDataFile=$crashLogDir\replay_pid%p.log" -jar $jarPath @args
exit $LASTEXITCODE
