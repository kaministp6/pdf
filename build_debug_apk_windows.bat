@echo off
setlocal
cd /d "%~dp0"
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle was not found on PATH.
  echo Open this project in Android Studio, sync Gradle, then choose Build ^> Build APK(s).
  echo If Gradle is installed, add it to PATH and run this script again.
  pause
  exit /b 1
)
call gradle --no-daemon assembleDebug
if errorlevel 1 (
  echo Build failed. Review the Gradle error output above.
  pause
  exit /b 1
)
echo.
echo APK output: app\build\outputs\apk\debug\app-debug.apk
pause
