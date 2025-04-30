@echo off
echo Meeting Transcriber App Runner
echo ===========================
echo.
echo 1. Run on Android
echo 2. Run on iOS (requires Mac)
echo 3. Run on Web
echo 4. Build Web (production)
echo.

set /p choice=Select platform (1-4): 

if "%choice%"=="1" (
  echo Starting Android app...
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
) else (
  echo Invalid choice. Please run again and select 1-4.
)

echo.
echo Press any key to exit...
pause > nul
