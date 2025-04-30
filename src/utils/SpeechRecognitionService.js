import { Platform } from 'react-native';
import { request, PERMISSIONS, RESULTS } from 'react-native-permissions';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Voice from '../../VoiceWeb';

class SpeechRecognitionService {
  constructor() {
    // Initialize voice listeners
    this.initVoiceListeners = this.initVoiceListeners.bind(this);
    this.removeVoiceListeners = this.removeVoiceListeners.bind(this);
    
    // Callbacks that will be set by the component using this service
    this.onSpeechStartCallback = null;
    this.onSpeechRecognizedCallback = null;
    this.onSpeechEndCallback = null;
    this.onSpeechErrorCallback = null;
    this.onSpeechResultsCallback = null;
    this.onSpeechPartialResultsCallback = null;
    this.onSpeechVolumeChangedCallback = null;
  }

  // Set up all the callback handlers
  setCallbacks({
    onSpeechStart,
    onSpeechRecognized,
    onSpeechEnd,
    onSpeechError,
    onSpeechResults,
    onSpeechPartialResults,
    onSpeechVolumeChanged,
  }) {
    this.onSpeechStartCallback = onSpeechStart;
    this.onSpeechRecognizedCallback = onSpeechRecognized;
    this.onSpeechEndCallback = onSpeechEnd;
    this.onSpeechErrorCallback = onSpeechError;
    this.onSpeechResultsCallback = onSpeechResults;
    this.onSpeechPartialResultsCallback = onSpeechPartialResults;
    this.onSpeechVolumeChangedCallback = onSpeechVolumeChanged;
  }

  // Initialize voice recognition event listeners
  initVoiceListeners() {
    Voice.onSpeechStart = (e) => {
      if (this.onSpeechStartCallback) this.onSpeechStartCallback(e);
    };
    Voice.onSpeechRecognized = (e) => {
      if (this.onSpeechRecognizedCallback) this.onSpeechRecognizedCallback(e);
    };
    Voice.onSpeechEnd = (e) => {
      if (this.onSpeechEndCallback) this.onSpeechEndCallback(e);
    };
    Voice.onSpeechError = (e) => {
      if (this.onSpeechErrorCallback) this.onSpeechErrorCallback(e);
    };
    Voice.onSpeechResults = (e) => {
      if (this.onSpeechResultsCallback) this.onSpeechResultsCallback(e);
    };
    Voice.onSpeechPartialResults = (e) => {
      if (this.onSpeechPartialResultsCallback) this.onSpeechPartialResultsCallback(e);
    };
    Voice.onSpeechVolumeChanged = (e) => {
      if (this.onSpeechVolumeChangedCallback) this.onSpeechVolumeChangedCallback(e);
    };
  }

  // Clean up voice recognition event listeners
  removeVoiceListeners() {
    try {
      Voice.destroy().then(Voice.removeAllListeners);
    } catch (e) {
      console.error('Error removing voice listeners:', e);
    }
  }

  // Request microphone permission based on platform
  async requestMicrophonePermission() {
    if (Platform.OS === 'ios') {
      try {
        const result = await request(PERMISSIONS.IOS.MICROPHONE);
        return result === RESULTS.GRANTED;
      } catch (error) {
        console.error('Error requesting iOS microphone permission:', error);
        return false;
      }
    } else if (Platform.OS === 'android') {
      try {
        const result = await request(PERMISSIONS.ANDROID.RECORD_AUDIO);
        return result === RESULTS.GRANTED;
      } catch (error) {
        console.error('Error requesting Android microphone permission:', error);
        return false;
      }
    }
    return true; // For web
  }

  // Start voice recognition
  async startListening(locale = 'en-US') {
    try {
      const hasPermission = await this.requestMicrophonePermission();
      
      if (!hasPermission) {
        throw new Error('Microphone permission was not granted');
      }
      
      await Voice.start(locale);
      return true;
    } catch (e) {
      console.error('Error starting speech recognition:', e);
      throw e;
    }
  }

  // Stop voice recognition
  async stopListening() {
    try {
      await Voice.stop();
      return true;
    } catch (e) {
      console.error('Error stopping speech recognition:', e);
      throw e;
    }
  }

  // Load saved transcripts from AsyncStorage
  async loadTranscripts() {
    try {
      const savedTranscripts = await AsyncStorage.getItem('transcripts');
      if (savedTranscripts !== null) {
        return JSON.parse(savedTranscripts);
      }
      return [];
    } catch (e) {
      console.error('Failed to load transcripts:', e);
      return [];
    }
  }

  // Save a new transcript to AsyncStorage
  async saveTranscript(text, transcripts) {
    try {
      const timestamp = new Date().toISOString();
      const newTranscript = {
        id: timestamp,
        text,
        date: timestamp,
      };
      const updatedTranscripts = [...transcripts, newTranscript];
      await AsyncStorage.setItem('transcripts', JSON.stringify(updatedTranscripts));
      return updatedTranscripts;
    } catch (e) {
      console.error('Failed to save transcript:', e);
      throw e;
    }
  }

  // Clear all saved transcripts
  async clearTranscripts() {
    try {
      await AsyncStorage.removeItem('transcripts');
      return [];
    } catch (e) {
      console.error('Failed to clear transcripts:', e);
      throw e;
    }
  }
}

// Export a singleton instance
export default new SpeechRecognitionService();
