import React from 'react';
import { Platform, SafeAreaView, StyleSheet } from 'react-native';
import HomeScreen from './src/screens/HomeScreen';
import AndroidSpeechRecognition from './src/components/AndroidSpeechRecognition';

const App = () => {
  return (
    <SafeAreaView style={styles.container}>
      {Platform.OS === 'android' ? (
        <AndroidSpeechRecognition />
      ) : (
        <HomeScreen />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5FCFF',
  },
});

export default App;
