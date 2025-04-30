@echo off
echo Installing dependencies for Meeting Transcriber app...

npm install

echo.
echo Installing Android-specific dependencies...
echo.

:: Install the main dependencies needed for Android
npm install --save react-native-voice@0.3.0
npm install --save react-native-permissions@3.8.0
npm install --save @react-native-async-storage/async-storage@1.18.2
npm install --save react-native-paper@5.10.6

echo.
echo Dependencies installed successfully!
echo.
echo Run 'run-app.bat' to start the application.
