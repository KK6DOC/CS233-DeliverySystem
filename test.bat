@echo off
setlocal
cd /d "%~dp0"
if not exist build\test-classes mkdir build\test-classes
javac --release 17 -Xlint:all -d build\test-classes src\*.java tests\*.java
if errorlevel 1 exit /b 1
java -ea -cp build\test-classes ProjectTest
if errorlevel 1 exit /b 1
java -ea -cp build\test-classes TerminalFlowTest
if errorlevel 1 exit /b 1
if "%~1"=="--desktop" (
    java -ea -cp build\test-classes DesktopFlowTest
    if errorlevel 1 exit /b 1
    java -cp build\test-classes UiPreview
    if errorlevel 1 exit /b 1
)
exit /b %errorlevel%
