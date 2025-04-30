import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

const LiveTranscription = ({ isListening, partialResults }) => {
  if (!isListening || !partialResults || partialResults.length === 0) {
    return null;
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Real-time Transcription:</Text>
      <View style={styles.transcriptionContainer}>
        <Text style={styles.transcriptionText}>{partialResults[0]}</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
  },
  title: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
    color: '#333',
  },
  transcriptionContainer: {
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    padding: 16,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  transcriptionText: {
    fontSize: 16,
    color: '#555',
    fontStyle: 'italic',
    lineHeight: 24,
  },
});

export default LiveTranscription;
