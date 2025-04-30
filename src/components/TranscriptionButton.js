import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Button } from 'react-native-paper';

const TranscriptionButton = ({ isListening, onStartListening, onStopListening }) => {
  return (
    <View style={styles.buttonContainer}>
      <Button
        mode="contained"
        icon={isListening ? 'stop' : 'microphone'}
        onPress={isListening ? onStopListening : onStartListening}
        style={[styles.button, isListening ? styles.stopButton : styles.startButton]}
        labelStyle={styles.buttonLabel}
      >
        {isListening ? 'Stop' : 'Start'} Transcribing
      </Button>
    </View>
  );
};

const styles = StyleSheet.create({
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    paddingVertical: 16,
  },
  button: {
    paddingHorizontal: 16,
    borderRadius: 8,
  },
  startButton: {
    backgroundColor: '#2196F3',
  },
  stopButton: {
    backgroundColor: '#F44336',
  },
  buttonLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: 'white',
  },
});

export default TranscriptionButton;
