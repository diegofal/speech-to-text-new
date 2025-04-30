import { Platform } from 'react-native';
import Voice from 'react-native-voice';

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

    Voice.onSpeechStart = this._onSpeechStart;
    Voice.onSpeechRecognized = this._onSpeechRecognized;
    Voice.onSpeechEnd = this._onSpeechEnd;
    Voice.onSpeechError = this._onSpeechError;
    Voice.onSpeechResults = this._onSpeechResults;
    Voice.onSpeechPartialResults = this._onSpeechPartialResults;
    Voice.onSpeechVolumeChanged = this._onSpeechVolumeChanged;
    
    this.isInitialized = true;
  }

  destroy() {
    if (Platform.OS === 'web') return;

    if (!this.isInitialized) return;

    Voice.destroy().then(Voice.removeAllListeners);
    this.isInitialized = false;
  }

  // Register listeners
  registerListener(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(callback);
  }

  // Remove listeners
  removeListener(event, callback) {
    if (!this.listeners[event]) return;
    this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
  }

  // Trigger callbacks for a specific event
  _triggerEvent(event, data) {
    if (!this.listeners[event]) return;
    this.listeners[event].forEach(callback => callback(data));
  }

  // Voice event handlers
  _onSpeechStart(e) {
    this._triggerEvent('onSpeechStart', e);
  }

  _onSpeechRecognized(e) {
    this._triggerEvent('onSpeechRecognized', e);
  }

  _onSpeechEnd(e) {
    this._triggerEvent('onSpeechEnd', e);
  }

  _onSpeechError(e) {
    this._triggerEvent('onSpeechError', e);
  }

  _onSpeechResults(e) {
    this._triggerEvent('onSpeechResults', e);
  }

  _onSpeechPartialResults(e) {
    this._triggerEvent('onSpeechPartialResults', e);
  }

  _onSpeechVolumeChanged(e) {
    this._triggerEvent('onSpeechVolumeChanged', e);
  }

  // Start voice recognition
  async start(locale = 'en-US') {
    if (Platform.OS === 'web') {
      console.error('Cannot use native voice recognition on web');
      throw new Error('Not supported on web');
    }

    try {
      if (!this.isInitialized) {
        this.initialize();
      }
      return await Voice.start(locale);
    } catch (e) {
      console.error('Error starting voice recognition:', e);
      throw e;
    }
  }

  // Stop voice recognition
  async stop() {
    if (Platform.OS === 'web') {
      return;
    }

    try {
      return await Voice.stop();
    } catch (e) {
      console.error('Error stopping voice recognition:', e);
      throw e;
    }
  }

  // Check if voice recognition is available
  async isAvailable() {
    if (Platform.OS === 'web') {
      return false;
    }

    // Voice library doesn't have a direct method to check availability
    // We can assume it's available if it initializes correctly
    return this.isInitialized;
  }
}

// Singleton instance
const voiceService = new VoiceService();
export default voiceService;
