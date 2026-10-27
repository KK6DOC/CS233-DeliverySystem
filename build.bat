@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
if not exist build\app mkdir build\app
javac --release 17 -d build\classes src\*.java
if errorlevel 1 exit /b 1
jar --create --file build\app\CS233DeliverySystem.jar --main-class Main -C build\classes .
if errorlevel 1 exit /b 1
copy /y Menu.txt build\app\Menu.txt >nul
if errorlevel 1 exit /b 1
echo Built build\app\CS233DeliverySystem.jar
