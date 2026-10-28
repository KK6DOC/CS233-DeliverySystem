@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
javac --release 17 -d build\classes src\*.java
if errorlevel 1 exit /b 1
java -cp build\classes Main %*
exit /b %errorlevel%
