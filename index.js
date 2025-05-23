/**
 * @format
 */

import {AppRegistry, Platform} from 'react-native';
import App from './App';
import {name as appName} from './app.json';

AppRegistry.registerComponent(appName, () => App);

// For web platform
if (Platform.OS === 'web') {
  AppRegistry.runApplication(appName, {
    rootTag: document.getElementById('root')
  });
}
