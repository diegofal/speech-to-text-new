// Web-specific implementation of voice recognition using Web Speech API
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

class VoiceWeb {
  constructor() {
    this.recognition = null;
    this.eventEmitter = new NativeEventEmitter();
    this.listeners = {};
    
    // Initialize if we're in a browser environment
    if (Platform.OS === 'web' && typeof window !== 'undefined') {
      this.initialize();
    }
  }

  // Initialize speech recognition
  initialize() {
    if (!window.SpeechRecognition && !window.webkitSpeechRecognition) {
      console.error('Speech recognition not supported in this browser');
      return false;
    }

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    this.recognition = new SpeechRecognition();
    this.recognition.continuous = true;
    this.recognition.interimResults = true;
    
    this.recognition.onstart = () => {
      this.emit('onSpeechStart', {});
    };
    
    this.recognition.onresult = (event) => {
      let interimTranscript = '';
      let finalTranscript = '';
      
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalTranscript += transcript;
        } else {
          interimTranscript += transcript;
        }
      }
      
      // Send partial results
      if (interimTranscript) {
        this.emit('onSpeechPartialResults', { value: [interimTranscript] });
      }
      
      // Send final results
      if (finalTranscript) {
        this.emit('onSpeechResults', { value: [finalTranscript] });
      }
    };
    
    this.recognition.onerror = (event) => {
      this.emit('onSpeechError', { error: event.error });
    };
    
    this.recognition.onend = () => {
      this.emit('onSpeechEnd', {});
    };
    
    return true;
  }

  // Start voice recognition
  start(locale = 'en-US') {
    if (!this.recognition) {
      if (!this.initialize()) {
        return Promise.reject('Speech recognition not supported');
      }
    }
    
    this.recognition.lang = locale;
    try {
      this.recognition.start();
      return Promise.resolve();
    } catch (error) {
      // Handle the case where recognition is already started
      if (error.name === 'InvalidStateError') {
        this.stop().then(() => {
          setTimeout(() => {
            this.recognition.start();
          }, 100);
        });
        return Promise.resolve();
      }
      return Promise.reject(error);
    }
  }

  // Stop voice recognition
  stop() {
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch (error) {
        console.error('Error stopping recognition:', error);
      }
    }
    return Promise.resolve();
  }

  // Clean up resources
  destroy() {
    if (this.recognition) {
      this.recognition.stop();
      this.recognition = null;
    }
    return Promise.resolve();
  }

  // Add event listener
  addEventListener(eventName, callback) {
    if (!this.listeners[eventName]) {
      this.listeners[eventName] = [];
    }
    this.listeners[eventName].push(callback);
    return { remove: () => this.removeEventListener(eventName, callback) };
  }

  // Remove event listener
  removeEventListener(eventName, callback) {
    if (this.listeners[eventName]) {
      this.listeners[eventName] = this.listeners[eventName].filter(cb => cb !== callback);
    }
  }

  // Remove all listeners
  removeAllListeners() {
    this.listeners = {};
  }

  // Emit event to all listeners
  emit(eventName, data) {
    if (this.listeners[eventName]) {
      this.listeners[eventName].forEach(callback => callback(data));
    }
  }

  // Define event handlers
  set onSpeechStart(fn) { this.addEventListener('onSpeechStart', fn); }
  set onSpeechRecognized(fn) { this.addEventListener('onSpeechRecognized', fn); }
  set onSpeechEnd(fn) { this.addEventListener('onSpeechEnd', fn); }
  set onSpeechError(fn) { this.addEventListener('onSpeechError', fn); }
  set onSpeechResults(fn) { this.addEventListener('onSpeechResults', fn); }
  set onSpeechPartialResults(fn) { this.addEventListener('onSpeechPartialResults', fn); }
  set onSpeechVolumeChanged(fn) { this.addEventListener('onSpeechVolumeChanged', fn); }
}

// Create a fallback object that will be used if Voice native module is not available
const VoiceFallback = new VoiceWeb();

// Try to use the native module if available, otherwise use our web implementation
let VoiceModule;
try {
  // Check if we have the native Voice module
  if (Platform.OS !== 'web' && NativeModules.Voice) {
    VoiceModule = NativeModules.Voice;
  } else {
    VoiceModule = VoiceFallback;
  }
} catch (e) {
  console.warn('Error initializing Voice module, falling back to web implementation:', e);
  VoiceModule = VoiceFallback;
}

export default VoiceModule;
