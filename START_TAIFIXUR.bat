@echo off

cd /d "%~dp0"

echo Starting server...

start /min "" ".\server\target\TaiFixurServer\TaiFixurServer.exe"

timeout /t 5 /nobreak >nul

echo Starting client...

start "" ".\client\target\TaiFixur\TaiFixur.exe"