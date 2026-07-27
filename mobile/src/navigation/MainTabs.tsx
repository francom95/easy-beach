import React from 'react';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import {useTheme} from '../theme/ThemeProvider';
import type {MainTabsParamList} from './types';
import {S09Home} from '../screens/zona3/S09Home';
import {S11Menu} from '../screens/zona3/S11Menu';
import {S21Pedidos} from '../screens/zona3/S21Pedidos';
import {S28Estadia} from '../screens/zona3/S28Estadia';

const Tab = createBottomTabNavigator<MainTabsParamList>();

/** 4 destinos fijos (etapa 07): Inicio / Menú / Pedidos / Estadía. */
export function MainTabs() {
  const {tokens} = useTheme();
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: tokens['color.primary'],
        tabBarInactiveTintColor: tokens['color.on-surface-muted'],
        tabBarStyle: {backgroundColor: tokens['color.surface'], borderTopColor: tokens['color.border']},
      }}>
      <Tab.Screen name="S09Home" component={S09Home} options={{title: 'Inicio'}} />
      <Tab.Screen name="S11Menu" component={S11Menu} options={{title: 'Menú'}} />
      <Tab.Screen name="S21Pedidos" component={S21Pedidos} options={{title: 'Pedidos'}} />
      <Tab.Screen name="S28Estadia" component={S28Estadia} options={{title: 'Estadía'}} />
    </Tab.Navigator>
  );
}
