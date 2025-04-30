# Meeting Transcriber

A React Native application that can transcribe conversations from meetings in real-time. This app works on web, iOS, and Android platforms.

## Features

- Real-time speech-to-text transcription
- Saves transcription history
- Works across multiple platforms (web, iOS, Android)
- Clean, intuitive user interface

## Prerequisites

- Node.js >= 16
- NPM or Yarn
- For iOS: Xcode, CocoaPods
- For Android: Android Studio, JDK

## Installation

```bash
# Install dependencies
npm install

# For iOS, install pods
npx pod-install ios
```

## Running the App

### iOS
```bash
npm run ios
```

### Android
```bash
npm run android
```

### Web
```bash
npm run web
```

## Required Permissions

This app requires microphone permissions to function properly:

- iOS: Microphone access
- Android: Record Audio permission

## How it Works

The app uses React Native Voice for speech recognition, which interfaces with the native speech recognition capabilities of iOS and Android. For web, it uses the Web Speech API.

Transcripts are saved locally using AsyncStorage and can be viewed in the app's history section.

## Technologies Used

- React Native
- React Native Voice
- React Native Paper (UI components)
- AsyncStorage (for local storage)
- React Native Permissions
