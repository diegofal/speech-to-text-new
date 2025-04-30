import { useState, useEffect } from 'react';
import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import voiceService from './VoiceService';

// Custom hook to handle voice recognition across platforms
const useVoiceRecognition = () => {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [partialTranscript, setPartialTranscript] = useState('');
  const [error, setError] = useState('');
  const [isNativeAvailable, setIsNativeAvailable] = useState(false);

  // Web Speech API recognition object
  let webRecognition = null;

  // Check if native voice recognition is available
  useEffect(() => {
    const checkNativeAvailability = async () => {
      if (Platform.OS !== 'web') {
        try {
          voiceService.initialize();
          setIsNativeAvailable(true);
        } catch (e) {
          console.error('Native voice recognition initialization failed:', e);
          setIsNativeAvailable(false);
        }
      }
    };

    checkNativeAvailability();

    // Cleanup function
    return () => {
      if (Platform.OS !== 'web') {
        voiceService.destroy();
      } else if (webRecognition) {
        webRecognition.stop();
      }
    };
  }, []);

  // Setup native voice recognition listeners
  useEffect(() => {
    if (Platform.OS !== 'web' && isNativeAvailable) {
      // Register event listeners for native voice recognition
      const onSpeechResults = (e) => {
        if (e.value && e.value.length > 0) {
          const newTranscript = e.value[0];
          setTranscript(prev => {
            const combinedTranscript = prev ? `${prev} ${newTranscript}` : newTranscript;
            return combinedTranscript;
          });
        }
      };

      const onSpeechPartialResults = (e) => {
        if (e.value && e.value.length > 0) {
          setPartialTranscript(e.value[0]);
        }
      };

      const onSpeechError = (e) => {
        setError(`Error: ${e.error?.message || JSON.stringify(e)}`);
        setIsListening(false);
      };

      const onSpeechEnd = () => {
        setIsListening(false);
        setPartialTranscript('');
      };

      // Register listeners
      voiceService.registerListener('onSpeechResults', onSpeechResults);
      voiceService.registerListener('onSpeechPartialResults', onSpeechPartialResults);
      voiceService.registerListener('onSpeechError', onSpeechError);
      voiceService.registerListener('onSpeechEnd', onSpeechEnd);

      // Cleanup function
      return () => {
        voiceService.removeListener('onSpeechResults', onSpeechResults);
        voiceService.removeListener('onSpeechPartialResults', onSpeechPartialResults);
        voiceService.removeListener('onSpeechError', onSpeechError);
        voiceService.removeListener('onSpeechEnd', onSpeechEnd);
      };
    }
  }, [isNativeAvailable]);

  // Start web speech recognition
  const startWebSpeechRecognition = () => {
    if (Platform.OS !== 'web') {
      setError('Web speech recognition is only available on web');
      return false;
    }

    try {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (!SpeechRecognition) {
        setError('Speech recognition not supported in this browser');
        return false;
      }

      webRecognition = new SpeechRecognition();
      webRecognition.continuous = true;
      webRecognition.interimResults = true;

      webRecognition.onstart = () => {
        setIsListening(true);
        setError('');
      };

      webRecognition.onresult = (event) => {
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

        if (interimTranscript) {
          setPartialTranscript(interimTranscript);
        }

        if (finalTranscript) {
          setTranscript(prev => prev + ' ' + finalTranscript);
          setPartialTranscript('');
        }
      };

      webRecognition.onerror = (event) => {
        setError(`Error: ${event.error}`);
        setIsListening(false);
      };

      webRecognition.onend = () => {
        setIsListening(false);
      };

      webRecognition.start();
      return true;
    } catch (err) {
      setError(`Failed to start speech recognition: ${err.message}`);
      return false;
    }
  };

  // Start native voice recognition
  const startNativeVoiceRecognition = async () => {
    if (Platform.OS === 'web' || !isNativeAvailable) {
      return false;
    }

    try {
      await voiceService.start('en-US');
      setIsListening(true);
      setError('');
      return true;
    } catch (e) {
      setError(`Error starting voice recognition: ${e.message || JSON.stringify(e)}`);
      return false;
    }
  };

  // Start recognition based on platform
  const startListening = async () => {
    if (Platform.OS !== 'web' && isNativeAvailable) {
      return startNativeVoiceRecognition();
    } else {
      return startWebSpeechRecognition();
    }
  };

  // Stop recognition based on platform
  const stopListening = async () => {
    if (Platform.OS !== 'web' && isNativeAvailable) {
      try {
        await voiceService.stop();
        setIsListening(false);
        return true;
      } catch (e) {
        setError(`Error stopping voice recognition: ${e.message || JSON.stringify(e)}`);
        return false;
      }
    } else if (webRecognition) {
      webRecognition.stop();
      setIsListening(false);
      return true;
    }
    return false;
  };

  // Save transcript to local storage
  const saveTranscript = async (text) => {
    if (!text || text.trim() === '') return;
    
    const timestamp = new Date().toISOString();
    const newTranscript = {
      id: timestamp,
      text: text.trim(),
      date: timestamp
    };

    try {
      let savedTranscripts = [];
      
      if (Platform.OS === 'web' && window.localStorage) {
        // Web storage
        savedTranscripts = JSON.parse(localStorage.getItem('transcripts') || '[]');
        savedTranscripts.push(newTranscript);
        localStorage.setItem('transcripts', JSON.stringify(savedTranscripts));
      } else {
        // Native AsyncStorage
        const storedTranscripts = await AsyncStorage.getItem('transcripts');
        savedTranscripts = storedTranscripts ? JSON.parse(storedTranscripts) : [];
        savedTranscripts.push(newTranscript);
        await AsyncStorage.setItem('transcripts', JSON.stringify(savedTranscripts));
      }
      
      return newTranscript;
    } catch (e) {
      console.error('Failed to save transcript:', e);
      throw e;
    }
  };

  // Load transcripts from storage
  const loadTranscripts = async () => {
    try {
      let savedTranscripts = [];
      
      if (Platform.OS === 'web' && window.localStorage) {
        // Web storage
        savedTranscripts = JSON.parse(localStorage.getItem('transcripts') || '[]');
      } else {
        // Native AsyncStorage
        const storedTranscripts = await AsyncStorage.getItem('transcripts');
        savedTranscripts = storedTranscripts ? JSON.parse(storedTranscripts) : [];
      }
      
      return savedTranscripts;
    } catch (e) {
      console.error('Failed to load transcripts:', e);
      return [];
    }
  };

  // Clear all transcripts
  const clearTranscripts = async () => {
    try {
      if (Platform.OS === 'web' && window.localStorage) {
        localStorage.removeItem('transcripts');
      } else {
        await AsyncStorage.removeItem('transcripts');
      }
      return true;
    } catch (e) {
      console.error('Failed to clear transcripts:', e);
      return false;
    }
  };

  return {
    isListening,
    transcript,
    partialTranscript,
    error,
    isNativeAvailable,
    startListening,
    stopListening,
    setTranscript,
    saveTranscript,
    loadTranscripts,
    clearTranscripts
  };
};

export default useVoiceRecognition;
