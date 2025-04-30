import React, { useState, useEffect } from 'react';
import { SafeAreaView, StyleSheet, View, Text, TextInput, ScrollView, Button, ActivityIndicator, Platform } from 'react-native';

// Import components
import TranscriptionButton from '../components/TranscriptionButton';
import LiveTranscription from '../components/LiveTranscription';
import TranscriptionList from '../components/TranscriptionList';

// Web Speech API fallback for when dependencies aren't available
const useBrowserSpeechRecognition = () => {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [partialTranscript, setPartialTranscript] = useState('');
  const [error, setError] = useState('');

  let recognition = null;

  const startListening = () => {
    if (Platform.OS !== 'web') {
      setError('Browser speech recognition is only available on web');
      return;
    }

    try {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (!SpeechRecognition) {
        setError('Speech recognition not supported in this browser');
        return;
      }

      recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;

      recognition.onstart = () => {
        setIsListening(true);
        setError('');
      };

      recognition.onresult = (event) => {
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

      recognition.onerror = (event) => {
        setError(`Error: ${event.error}`);
        setIsListening(false);
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognition.start();
    } catch (err) {
      setError(`Failed to start speech recognition: ${err.message}`);
    }
  };

  const stopListening = () => {
    if (recognition) {
      recognition.stop();
      setIsListening(false);
    }
  };

  return {
    isListening,
    transcript,
    partialTranscript,
    error,
    startListening,
    stopListening
  };
};

const HomeScreen = () => {
  // State variables
  const [transcripts, setTranscripts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [usingFallback, setUsingFallback] = useState(false);
  
  // Try to use the browser speech recognition fallback if we're on web
  const {
    isListening,
    transcript,
    partialTranscript,
    error,
    startListening,
    stopListening
  } = useBrowserSpeechRecognition();
  
  // Effect to save the transcript when it changes and is not empty
  useEffect(() => {
    if (transcript && transcript.trim() !== '') {
      saveTranscript(transcript);
    }
  }, [transcript]);

  // Simple save function without external dependencies
  const saveTranscript = (text) => {
    const timestamp = new Date().toISOString();
    const newTranscript = {
      id: timestamp,
      text: text.trim(),
      date: timestamp
    };
    
    setTranscripts(prev => [...prev, newTranscript]);
    
    // Try to use localStorage on web
    if (Platform.OS === 'web' && window.localStorage) {
      try {
        const savedTranscripts = JSON.parse(localStorage.getItem('transcripts') || '[]');
        savedTranscripts.push(newTranscript);
        localStorage.setItem('transcripts', JSON.stringify(savedTranscripts));
      } catch (e) {
        console.error('Failed to save to localStorage:', e);
      }
    }
  };

  // Clear all transcripts
  const clearTranscripts = () => {
    setTranscripts([]);
    if (Platform.OS === 'web' && window.localStorage) {
      localStorage.removeItem('transcripts');
    }
  };

  // Load saved transcripts if available (web only)
  useEffect(() => {
    if (Platform.OS === 'web' && window.localStorage) {
      try {
        const savedTranscripts = JSON.parse(localStorage.getItem('transcripts') || '[]');
        setTranscripts(savedTranscripts);
      } catch (e) {
        console.error('Failed to load from localStorage:', e);
      }
    }
    setLoading(false);
  }, []);

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#0000ff" />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Meeting Transcriber</Text>
        {transcripts.length > 0 && (
          <Button title="Clear All" onPress={clearTranscripts} />
        )}
      </View>

      <View style={styles.statusContainer}>
        <Text style={styles.statusText}>
          {isListening ? 'Listening...' : 'Not listening'}
        </Text>
        {error !== '' && <Text style={styles.errorText}>Error: {error}</Text>}
      </View>

      <View style={styles.buttonContainer}>
        <Button
          title={isListening ? 'Stop Transcribing' : 'Start Transcribing'}
          onPress={isListening ? stopListening : startListening}
        />
      </View>

      {isListening && partialTranscript && (
        <View style={styles.liveContainer}>
          <Text style={styles.liveTitle}>Current:</Text>
          <Text style={styles.liveText}>{partialTranscript}</Text>
        </View>
      )}

      <View style={styles.divider} />

      <Text style={styles.sectionTitle}>
        {transcripts.length > 0 ? 'Saved Transcripts:' : 'No saved transcripts yet'}
      </Text>

      <ScrollView style={styles.transcriptContainer}>
        {transcripts
          .slice()
          .reverse()
          .map((item) => (
            <View key={item.id} style={styles.transcriptItem}>
              <Text style={styles.transcriptDate}>
                {new Date(item.date).toLocaleString()}
              </Text>
              <Text style={styles.transcriptText}>{item.text}</Text>
            </View>
          ))}
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#2196F3',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: 'white',
  },
  statusContainer: {
    padding: 10,
    alignItems: 'center',
  },
  statusText: {
    fontSize: 16,
    color: '#333',
  },
  errorText: {
    color: 'red',
    fontSize: 14,
    marginTop: 5,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    paddingVertical: 16,
  },
  liveContainer: {
    padding: 16,
    backgroundColor: '#e3f2fd',
    margin: 10,
    borderRadius: 8,
  },
  liveTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  liveText: {
    fontSize: 16,
    fontStyle: 'italic',
  },
  divider: {
    height: 1,
    backgroundColor: '#ddd',
    marginVertical: 10,
    marginHorizontal: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  transcriptContainer: {
    flex: 1,
    paddingHorizontal: 10,
  },
  transcriptItem: {
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 15,
    marginBottom: 10,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 1.41,
  },
  transcriptDate: {
    fontSize: 12,
    color: '#888',
    marginBottom: 5,
  },
  transcriptText: {
    fontSize: 16,
  },
});

export default HomeScreen;
