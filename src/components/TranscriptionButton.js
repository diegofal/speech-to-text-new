import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Button } from 'react-native-paper';

const TranscriptionButton = ({ isListening, onStartListening, onStopListening, isEnabled }) => {
  return (
    <View style={styles.buttonContainer}>
      {isListening ? (
        <Button
          mode="contained"
          icon="stop"
          onPress={onStopListening}
          style={[styles.button, styles.stopButton]}
          labelStyle={styles.buttonLabel}
          disabled={!isEnabled}
        >
          Stop Transcribing
        </Button>
      ) : (
        <Button
          mode="contained"
          icon="microphone"
          onPress={onStartListening}
          style={[styles.button, styles.startButton]}
          labelStyle={styles.buttonLabel}
          disabled={!isEnabled}
        >
          Start Transcribing
        </Button>
      )}
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
    minWidth: 200,
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
