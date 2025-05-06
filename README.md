# Meeting Transcriber

A powerful React Native application that provides real-time speech-to-text transcription for meetings across web, iOS, and Android platforms. Perfect for meetings, interviews, lectures, and any scenario where you need accurate speech transcription.

## Current Features

### Core Functionality
- Real-time speech-to-text transcription with streaming support
- Offline recognition on Android using Vosk
- Multiple recognition engines:
  - Android built-in speech recognition
  - Vosk (offline-capable)
  - Whisper* (coming soon)
- Multi-language support:
  - English 🇺🇸
  - Spanish 🇪🇸
  - (More languages coming soon)

### User Interface
- Cross-platform responsive design (web, iOS & Android)
- Dark-mode aware Material design using React-Native-Paper
- Intuitive and clean user interface
- Platform-specific optimizations

### Data Management
- Local storage of transcription history
- Export transcripts to .txt format
- Secure data handling
- Easy transcript browsing and management

## Upcoming Features

### Enhanced Transcription
- [ ] Advanced punctuation and formatting
- [ ] Timestamp markers for important moments
- [ ] Additional language support
- [ ] Noise reduction and audio enhancement

### Collaboration
- [ ] Real-time transcript sharing
- [ ] Collaborative editing
- [ ] Comments and annotations
- [ ] AI-powered meeting summaries

### User Experience
- [ ] Custom themes and UI customization
- [ ] Gesture controls
- [ ] Voice commands
- [ ] Enhanced accessibility features

### Export Options
- [ ] Multiple format support (PDF, DOCX, SRT)
- [ ] Custom export templates
- [ ] Batch export functionality
- [ ] Note-taking app integrations

### Analytics
- [ ] Meeting duration statistics
- [ ] Speaking time analysis
- [ ] Keyword extraction
- [ ] Sentiment analysis

## Features

- Real-time speech-to-text transcription (streamed as you speak)
- Offline recognition on Android using Vosk
- Multiple recognizer engines (Android built-in, Vosk, Whisper*)
- English 🇺🇸 and Spanish 🇪🇸 support (easily extendable)
- Saves and browses transcription history
- Export transcripts to **.txt** from the History screen
- Responsive UI that works on web, iOS & Android
- Dark-mode aware Material design (React-Native-Paper)
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
- Vosk speech-to-text (Android offline)
- Kotlin native module bridge
- Webpack + React-Native-Web

## Project Structure

| Path | Purpose |
|------|---------|
| `src/` | Shared JS/TS components and business logic |
| `src/components/*.(js \| web.js \| android.js)` | Platform-split UI and wrappers |
| `android/` | Native Android code (Kotlin Vosk recogniser) |
| `ios/` | iOS project (placeholder) |
| `public/` | Web entry (`index.html`) served by webpack-dev-server |

## Roadmap / Nice-to-have

- 🔈 Speaker diarisation (who said what?)
- 🌐 Automatic language detection
- ☁️ Cloud sync (Supabase / Firebase) so transcripts follow you across devices
- 📄 Export to PDF / DOCX
- 📊 Analytics dashboard (talk-time per speaker, word clouds)
- 🖥️ PWA packaging for installable web experience
- ✨ Whisper on-device support once released for RN (iOS & Android)

*PRs welcome!*
