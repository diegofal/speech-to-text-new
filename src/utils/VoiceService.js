import { Platform } from 'react-native';
import Voice from 'react-native-voice';
import NativeSpeechRecognition from './NativeSpeechRecognition';

class VoiceService {
  constructor() {
    this.listeners = {};
    this.isInitialized = false;
    this._onSpeechStart = this._onSpeechStart.bind(this);
    this._onSpeechRecognized = this._onSpeechRecognized.bind(this);
    this._onSpeechEnd = this._onSpeechEnd.bind(this);
    this._onSpeechError = this._onSpeechError.bind(this);
    this._onSpeechResults = this._onSpeechResults.bind(this);
    this._onSpeechPartialResults = this._onSpeechPartialResults.bind(this);
    this._onSpeechVolumeChanged = this._onSpeechVolumeChanged.bind(this);
  }

  initialize() {
    if (Platform.OS === 'web') {
      console.log('Voice initialization skipped on web platform');
      return;
    }

    if (this.isInitialized) return;

    if (Platform.OS === 'android') {
      // Use our native Android speech recognition module
      NativeSpeechRecognition.initialize().then(initialized => {
        if (initialized) {
          // Set up event listeners for the native module
          NativeSpeechRecognition.addListener('onSpeechStart', this._onSpeechStart);
          NativeSpeechRecognition.addListener('onSpeechRecognized', this._onSpeechRecognized);
          NativeSpeechRecognition.addListener('onSpeechEnd', this._onSpeechEnd);
          NativeSpeechRecognition.addListener('onSpeechError', this._onSpeechError);
          NativeSpeechRecognition.addListener('onSpeechResults', this._onSpeechResults);
          NativeSpeechRecognition.addListener('onSpeechPartialResults', this._onSpeechPartialResults);
          NativeSpeechRecognition.addListener('onSpeechVolumeChanged', this._onSpeechVolumeChanged);
          this.isInitialized = true;
        }
      });
    } else {
      // Use react-native-voice for iOS
      Voice.onSpeechStart = this._onSpeechStart;
      Voice.onSpeechRecognized = this._onSpeechRecognized;
      Voice.onSpeechEnd = this._onSpeechEnd;
      Voice.onSpeechError = this._onSpeechError;
      Voice.onSpeechResults = this._onSpeechResults;
      Voice.onSpeechPartialResults = this._onSpeechPartialResults;
      Voice.onSpeechVolumeChanged = this._onSpeechVolumeChanged;
      this.isInitialized = true;
    }
  }

  destroy() {
    if (Platform.OS === 'web') return;

    if (!this.isInitialized) return;

    if (Platform.OS === 'android') {
      NativeSpeechRecognition.destroy();
    } else {
      Voice.destroy().then(Voice.removeAllListeners);
    }
    this.isInitialized = false;
  }

  // Register listeners
  registerListener(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(callback);
  }

  // Remove listener
  removeListener(event, callback) {
    if (!this.listeners[event]) return;

    const index = this.listeners[event].indexOf(callback);
    if (index !== -1) {
      this.listeners[event].splice(index, 1);
    }
  }

  // Start speech recognition
  async start(locale = 'en-US') {
    if (Platform.OS === 'web') {
      throw new Error('Speech recognition is not supported on web platform');
    }

    if (!this.isInitialized) {
      this.initialize();
    }

    if (Platform.OS === 'android') {
      return await NativeSpeechRecognition.start(locale);
    } else {
      return await Voice.start(locale);
    }
  }

  // Stop speech recognition
  async stop() {
    if (Platform.OS === 'web') return;

    if (!this.isInitialized) return;

    if (Platform.OS === 'android') {
      return await NativeSpeechRecognition.stop();
    } else {
      return await Voice.stop();
    }
  }

  // Event handlers
  _onSpeechStart(e) {
    this._notifyListeners('onSpeechStart', e);
  }

  _onSpeechRecognized(e) {
    this._notifyListeners('onSpeechRecognized', e);
  }

  _onSpeechEnd(e) {
    this._notifyListeners('onSpeechEnd', e);
  }

  _onSpeechError(e) {
    this._notifyListeners('onSpeechError', e);
  }

  _onSpeechResults(e) {
    this._notifyListeners('onSpeechResults', e);
  }

  _onSpeechPartialResults(e) {
    this._notifyListeners('onSpeechPartialResults', e);
  }

  _onSpeechVolumeChanged(e) {
    this._notifyListeners('onSpeechVolumeChanged', e);
  }

  _notifyListeners(event, data) {
    if (!this.listeners[event]) return;

    this.listeners[event].forEach(callback => {
      try {
        callback(data);
      } catch (error) {
        console.error(`Error in ${event} listener:`, error);
      }
    });
  }
}

export default new VoiceService();
