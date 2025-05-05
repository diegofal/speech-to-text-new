import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, PermissionsAndroid, Platform } from 'react-native';
import NativeSpeechRecognition from '../utils/NativeSpeechRecognition';

const AndroidSpeechRecognition = ({ onTranscriptChange }) => {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [partialTranscript, setPartialTranscript] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    // Initialize speech recognition
    const initializeSpeechRecognition = async () => {
      try {
        await NativeSpeechRecognition.initialize();
        
        // Check for permission
        const hasPermission = await NativeSpeechRecognition.checkPermission();
        if (!hasPermission) {
          const granted = await NativeSpeechRecognition.requestPermission();
          if (!granted) {
            setError('Microphone permission denied');
          }
        }
      } catch (err) {
        setError(`Initialization error: ${err.message}`);
      }
    };

    initializeSpeechRecognition();

    // Set up event listeners
    const onSpeechResults = (event) => {
      if (event.value && event.value.length > 0) {
        const newTranscript = event.value[0];
        setTranscript(prev => {
          const combinedTranscript = prev ? `${prev} ${newTranscript}` : newTranscript;
          // Notify parent component of transcript change
          if (onTranscriptChange) {
            onTranscriptChange(combinedTranscript);
          }
          return combinedTranscript;
        });
      }
    };

    const onSpeechPartialResults = (event) => {
      if (event.value && event.value.length > 0) {
        setPartialTranscript(event.value[0]);
      }
    };

    const onSpeechError = (event) => {
      setError(`Error: ${event.error?.message || JSON.stringify(event.error)}`);
      setIsListening(false);
    };

    const onSpeechEnd = () => {
      setIsListening(false);
    };

    // Add listeners
    NativeSpeechRecognition.addListener('onSpeechResults', onSpeechResults);
    NativeSpeechRecognition.addListener('onSpeechPartialResults', onSpeechPartialResults);
    NativeSpeechRecognition.addListener('onSpeechError', onSpeechError);
    NativeSpeechRecognition.addListener('onSpeechEnd', onSpeechEnd);

    // Cleanup function
    return () => {
      NativeSpeechRecognition.removeAllListeners('onSpeechResults');
      NativeSpeechRecognition.removeAllListeners('onSpeechPartialResults');
      NativeSpeechRecognition.removeAllListeners('onSpeechError');
      NativeSpeechRecognition.removeAllListeners('onSpeechEnd');
      NativeSpeechRecognition.destroy();
    };
  }, []);

  const startListening = async () => {
    setError('');
    setPartialTranscript('');
    
    try {
      await NativeSpeechRecognition.start('en-US');
      setIsListening(true);
    } catch (err) {
      setError(`Start error: ${err.message}`);
    }
  };

  const stopListening = async () => {
    try {
      await NativeSpeechRecognition.stop();
      setIsListening(false);
    } catch (err) {
      setError(`Stop error: ${err.message}`);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Android Speech Recognition</Text>
      
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
      
      <TouchableOpacity
        style={[styles.button, isListening ? styles.stopButton : styles.startButton]}
        onPress={isListening ? stopListening : startListening}
      >
        <Text style={styles.buttonText}>
          {isListening ? 'Stop Listening' : 'Start Listening'}
        </Text>
      </TouchableOpacity>
      
      {isListening && partialTranscript ? (
        <View style={styles.transcriptContainer}>
          <Text style={styles.label}>Listening:</Text>
          <Text style={styles.partialText}>{partialTranscript}</Text>
        </View>
      ) : null}
      
      {transcript ? (
        <View style={styles.transcriptContainer}>
          <Text style={styles.label}>Transcript:</Text>
          <Text style={styles.transcriptText}>{transcript}</Text>
        </View>
      ) : null}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#2196F3',
  },
  button: {
    width: 200,
    height: 50,
    borderRadius: 25,
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: 20,
  },
  startButton: {
    backgroundColor: '#4CAF50',
  },
  stopButton: {
    backgroundColor: '#F44336',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  transcriptContainer: {
    width: '100%',
    padding: 15,
    backgroundColor: 'white',
    borderRadius: 10,
    marginTop: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3,
  },
  label: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 5,
    color: '#555',
  },
  partialText: {
    fontSize: 16,
    color: '#666',
    fontStyle: 'italic',
  },
  transcriptText: {
    fontSize: 16,
    color: '#333',
  },
  errorText: {
    color: 'red',
    marginBottom: 10,
  },
});

export default AndroidSpeechRecognition;
