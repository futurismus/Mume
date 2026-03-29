@echo off
setlocal

:: --- CONFIGURATION ---
:: Updated to user Documents folder to avoid "Access Denied" errors
set MAX_JAVA_CLASSES=C:\Users\bened\Documents\Max 8\Packages\Mume\java-classes
:: ---------------------

echo [1/2] Building project with Gradle...
call gradlew.bat classes

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Gradle build failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Copying classes to Max...
if not exist "%MAX_JAVA_CLASSES%" mkdir "%MAX_JAVA_CLASSES%"

:: Copy all .class files from the build directory to Max
xcopy /S /Y "build\classes\java\main\*" "%MAX_JAVA_CLASSES%\"

echo.
echo Deployment successful!
echo Target: %MAX_JAVA_CLASSES%
echo.
pause
