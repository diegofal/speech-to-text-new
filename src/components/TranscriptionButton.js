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
        style={styles.button}
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
  buttonLabel: {
    fontSize: 16,
    fontWeight: '600',
  },
});

export default TranscriptionButton;
