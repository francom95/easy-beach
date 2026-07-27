/**
 * EasyBeach - app del cliente (etapa 16).
 * @format
 */

import React from 'react';
import {StatusBar} from 'react-native';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import {NavigationContainer} from '@react-navigation/native';
import {QueryClientProvider} from '@tanstack/react-query';
import {queryClient} from './src/api/queryClient';
import {ThemeProvider} from './src/theme/ThemeProvider';
import {RootNavigator} from './src/navigation/RootNavigator';
import {navigationRef} from './src/navigation/navigationRef';
import {RealtimeProvider} from './src/realtime/RealtimeProvider';

function App(): React.JSX.Element {
  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <RealtimeProvider>
            <StatusBar barStyle="dark-content" />
            <NavigationContainer ref={navigationRef}>
              <RootNavigator />
            </NavigationContainer>
          </RealtimeProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}

export default App;
