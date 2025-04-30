@echo off
echo Meeting Transcriber App Runner
echo ===========================
echo.
echo 1. Run on Android
echo 2. Run on iOS (requires Mac)
echo 3. Run on Web
echo 4. Build Web (production)
echo 5. Install Dependencies
echo.

set /p choice=Select platform (1-5): 

if "%choice%"=="1" (
  echo Starting Android app...
  echo.
  echo Note: Make sure you have:
  echo  - Android SDK installed and ANDROID_HOME environment variable set
  echo  - A running Android emulator or connected Android device
  echo.
  echo Starting app on Android...
  npm run android
) else if "%choice%"=="2" (
  echo Starting iOS app...
  echo Note: This requires a Mac. If you're on Windows, this won't work.
  npm run ios
) else if "%choice%"=="3" (
  echo Starting Web app...
  npm run web
) else if "%choice%"=="4" (
  echo Building Web app for production...
  npm run build-web
  echo Build completed. Files are in the dist/ directory.
) else if "%choice%"=="5" (
  echo Installing dependencies...
  call install-dependencies.bat
) else (
  echo Invalid choice. Please run again and select 1-5.
)

echo.
echo Press any key to exit...
pause > nul
