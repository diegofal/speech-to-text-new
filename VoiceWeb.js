// Web-specific implementation of voice recognition using Web Speech API
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

class VoiceWeb {
  constructor() {
    this.recognition = null;
    this.eventEmitter = new NativeEventEmitter();
    this.listeners = {};
    this.isContinuous = true;
    this.lastFinalTranscript = '';
    
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
    this.recognition.maxAlternatives = 1;
    
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
      
      // Send partial results immediately
      if (interimTranscript) {
        this.emit('onSpeechPartialResults', { value: [interimTranscript] });
      }
      
      // Send final results
      if (finalTranscript) {
        // Only send the new part of the transcript
        const newTranscript = finalTranscript.substring(this.lastFinalTranscript.length);
        if (newTranscript) {
          this.emit('onSpeechResults', { value: [newTranscript] });
          this.lastFinalTranscript = finalTranscript;
        }
      }
    };
    
    this.recognition.onerror = (event) => {
      this.emit('onSpeechError', { error: event.error });
      // Restart recognition if it was an error that can be recovered from
      if (event.error !== 'no-speech' && event.error !== 'aborted') {
        this.restartRecognition();
      }
    };
    
    this.recognition.onend = () => {
      this.emit('onSpeechEnd', {});
      // Restart recognition if we're in continuous mode
      if (this.isContinuous) {
        this.restartRecognition();
      }
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
    this.lastFinalTranscript = '';
    this.isContinuous = true;
    
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
      this.isContinuous = false;
      this.recognition.stop();
    }
    return Promise.resolve();
  }

  // Restart recognition after a short delay
  restartRecognition() {
    if (this.isContinuous) {
      setTimeout(() => {
        try {
          this.recognition.start();
        } catch (error) {
          console.warn('Failed to restart recognition:', error);
        }
      }, 100);
    }
  }

  // Add event listener
  addListener(eventName, callback) {
    if (!this.listeners[eventName]) {
      this.listeners[eventName] = [];
    }
    this.listeners[eventName].push(callback);
  }

  // Remove event listener
  removeListener(eventName, callback) {
    if (this.listeners[eventName]) {
      this.listeners[eventName] = this.listeners[eventName].filter(cb => cb !== callback);
    }
  }

  // Emit event to all listeners
  emit(eventName, data) {
    if (this.listeners[eventName]) {
      this.listeners[eventName].forEach(callback => callback(data));
    }
  }

  // Destroy the recognition instance
  destroy() {
    if (this.recognition) {
      this.isContinuous = false;
      this.recognition.stop();
      this.recognition = null;
    }
  }
}

export default new VoiceWeb();
