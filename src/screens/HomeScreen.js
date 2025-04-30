import React, { useState, useEffect } from 'react';
import { SafeAreaView, StyleSheet, View, Text, TextInput, ScrollView, Button, ActivityIndicator, Platform } from 'react-native';

// Import components
import TranscriptionButton from '../components/TranscriptionButton';
import LiveTranscription from '../components/LiveTranscription';
import TranscriptionList from '../components/TranscriptionList';

// Import custom hook for voice recognition
import useVoiceRecognition from '../utils/useVoiceRecognition';

const HomeScreen = () => {
  // State variables
  const [transcripts, setTranscripts] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Use our custom voice recognition hook
  const {
    isListening,
    transcript,
    partialTranscript,
    error,
    startListening,
    stopListening,
    saveTranscript,
    loadTranscripts,
    clearTranscripts
  } = useVoiceRecognition();
  
  // Effect to save the transcript when it changes and is not empty
  useEffect(() => {
    if (transcript && transcript.trim() !== '') {
      saveTranscript(transcript).then(newTranscript => {
        if (newTranscript) {
          setTranscripts(prev => [...prev, newTranscript]);
        }
      });
    }
  }, [transcript]);

  // Load saved transcripts if available 
  useEffect(() => {
    const fetchTranscripts = async () => {
      try {
        const savedTranscripts = await loadTranscripts();
        setTranscripts(savedTranscripts);
      } catch (e) {
        console.error('Failed to load transcripts:', e);
      } finally {
        setLoading(false);
      }
    };

    fetchTranscripts();
  }, []);

  // Handler for clearing all transcripts
  const handleClearTranscripts = async () => {
    const success = await clearTranscripts();
    if (success) {
      setTranscripts([]);
    }
  };

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
          <Button title="Clear All" onPress={handleClearTranscripts} />
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
