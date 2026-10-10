@echo off
REM =========================================================
REM Employee Time Management System - Run Script (Windows)
REM =========================================================

echo =========================================================
echo   Starting Employee Time Management System (Windows)
echo =========================================================

REM Check for Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java is not installed or not in your PATH.
    echo Please install Java JDK 17 or higher.
    pause
    exit /b 1
)

REM Create output directory
if not exist "bin" mkdir bin

REM Copy resources to classpath
if exist "src\main\resources" (
    xcopy /s /y /q "src\main\resources\*" "bin\" >nul 2>nul
)

REM Compile source files
echo Compiling Java source files...
dir /s /b src\main\java\*.java > sources.txt
javac -d bin -cp "lib\*;src\main\resources" @sources.txt

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    del sources.txt >nul 2>nul
    pause
    exit /b 1
)

del sources.txt >nul 2>nul
echo [OK] Compilation successful.
echo Launching GUI Application...

REM Run Main class (Windows uses semicolon ';' for classpath)
java -cp "bin;lib\*" com.timemanagement.Main

pause
