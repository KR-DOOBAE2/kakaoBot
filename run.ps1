$ErrorActionPreference = 'Stop'
if (-not $env:OWNER_SECRET) { throw 'OWNER_SECRET 환경 변수를 먼저 설정해주세요.' }
Set-Location $PSScriptRoot
java -jar bot-app/build/libs/bot-app-0.1.0.jar
