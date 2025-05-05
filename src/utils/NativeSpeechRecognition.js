import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const { SpeechRecognitionModule } = NativeModules;

class NativeSpeechRecognition {
  constructor() {
    this.isInitialized = false;
    this.eventEmitter = null;
    this.listeners = {};
  }

  async initialize() {
    if (Platform.OS !== 'android') {
      console.warn('NativeSpeechRecognition is only available on Android');
      return false;
    }

    if (this.isInitialized) {
      return true;
    }

    try {
      const result = await SpeechRecognitionModule.initialize();
      if (result) {
        this.eventEmitter = new NativeEventEmitter(SpeechRecognitionModule);
        this.isInitialized = true;
        return true;
      }
      return false;
    } catch (error) {
      console.error('Failed to initialize speech recognition:', error);
      return false;
    }
  }

  async checkPermission() {
    if (!this.isInitialized) {
      await this.initialize();
    }
    try {
      return await SpeechRecognitionModule.checkPermission();
    } catch (error) {
      console.error('Failed to check permission:', error);
      return false;
    }
  }

  async requestPermission() {
    if (!this.isInitialized) {
      await this.initialize();
    }
    try {
      return await SpeechRecognitionModule.requestPermission();
    } catch (error) {
      console.error('Failed to request permission:', error);
      return false;
    }
  }

  async start(locale = 'en-US') {
    if (!this.isInitialized) {
      const initialized = await this.initialize();
      if (!initialized) {
        throw new Error('Failed to initialize speech recognition');
      }
    }

    const hasPermission = await this.checkPermission();
    if (!hasPermission) {
      const permissionGranted = await this.requestPermission();
      if (!permissionGranted) {
        throw new Error('Permission not granted for speech recognition');
      }
    }

    try {
      return await SpeechRecognitionModule.startListening(locale);
    } catch (error) {
      console.error('Failed to start speech recognition:', error);
      throw error;
    }
  }

  async stop() {
    if (!this.isInitialized) {
      return false;
    }

    try {
      return await SpeechRecognitionModule.stopListening();
    } catch (error) {
      console.error('Failed to stop speech recognition:', error);
      return false;
    }
  }

  async destroy() {
    if (!this.isInitialized) {
      return false;
    }

    try {
      // Remove all listeners
      Object.keys(this.listeners).forEach(eventType => {
        this.listeners[eventType].forEach(subscription => {
          subscription.remove();
        });
      });
      this.listeners = {};

      const result = await SpeechRecognitionModule.destroy();
      this.isInitialized = false;
      return result;
    } catch (error) {
      console.error('Failed to destroy speech recognition:', error);
      return false;
    }
  }

  // Event handling methods
  addListener(eventType, callback) {
    if (!this.isInitialized || !this.eventEmitter) {
      console.warn('Speech recognition not initialized');
      return { remove: () => {} };
    }

    const subscription = this.eventEmitter.addListener(eventType, callback);
    
    if (!this.listeners[eventType]) {
      this.listeners[eventType] = [];
    }
    this.listeners[eventType].push(subscription);
    
    return subscription;
  }

  removeListener(eventType, subscription) {
    if (!this.listeners[eventType]) {
      return;
    }

    const index = this.listeners[eventType].indexOf(subscription);
    if (index !== -1) {
      subscription.remove();
      this.listeners[eventType].splice(index, 1);
    }
  }

  removeAllListeners(eventType) {
    if (!eventType) {
      // Remove all listeners for all event types
      Object.keys(this.listeners).forEach(type => {
        this.listeners[type].forEach(subscription => {
          subscription.remove();
        });
      });
      this.listeners = {};
    } else if (this.listeners[eventType]) {
      // Remove all listeners for a specific event type
      this.listeners[eventType].forEach(subscription => {
        subscription.remove();
      });
      this.listeners[eventType] = [];
    }
  }
}

export default new NativeSpeechRecognition();
