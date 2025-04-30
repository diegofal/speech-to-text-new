# Meeting Transcriber App - Usage Guide

## Overview

This React Native application allows you to transcribe conversations from meetings in real-time. It works across multiple platforms (web, iOS, and Android) and provides an intuitive interface for recording and saving transcriptions.

## Features

- Real-time speech-to-text transcription
- Saved transcription history
- Cross-platform compatibility (web, iOS, Android)
- Simple, intuitive UI

## Prerequisites

Before getting started, make sure you have the following installed:

- Node.js (version 16 or higher)
- npm or yarn
- For iOS development: Xcode and CocoaPods
- For Android development: Android Studio and JDK

## Installation

1. Clone or download this repository
2. Navigate to the project directory
3. Run the installation script or install dependencies manually:

```bash
# Using the provided script
./install-dependencies.bat

# Or manually install dependencies
npm install

# For iOS, install pods
cd ios && pod install && cd ..
```

## Running the App

### Android

```bash
npm run android
```

### iOS

```bash
npm run ios
```

### Web

```bash
npm run web
```

## How to Use

1. Launch the app on your preferred platform
2. Grant microphone permission when prompted
3. Press the "Start Transcribing" button to begin recording
4. Speak clearly into your device's microphone
5. Real-time transcription will appear on the screen
6. Press "Stop Transcribing" when you're done
7. The transcription will be saved automatically and appear in the list below
8. You can clear all saved transcriptions by pressing the trash icon in the top-right corner

## Troubleshooting

### Permission Issues

- Make sure you've granted microphone access to the app
- For iOS, check Settings > Privacy > Microphone
- For Android, check Settings > Apps > Meeting Transcriber > Permissions

### Speech Recognition Not Working

- Ensure you have an active internet connection (required for some speech recognition services)
- Try speaking more clearly and closer to the microphone
- Check that your device's microphone is functioning properly

## Technical Details

This app uses:

- React Native for cross-platform compatibility
- React Native Voice for native speech recognition
- Web Speech API for web-based speech recognition
- AsyncStorage for local storage of transcriptions
- React Native Paper for UI components

## Project Structure

```
├── android/                 # Android-specific code
├── ios/                    # iOS-specific code
├── src/
│   ├── components/         # Reusable React components
│   │   ├── LiveTranscription.js
│   │   ├── TranscriptionButton.js
│   │   └── TranscriptionList.js
│   ├── screens/           # Full-screen components
│   │   └── HomeScreen.js
│   └── utils/             # Helper functions and services
│       └── SpeechRecognitionService.js
├── web/                   # Web-specific files
├── App.js                 # Entry point
├── VoiceWeb.js            # Cross-platform voice recognition
└── package.json           # Project dependencies
```
