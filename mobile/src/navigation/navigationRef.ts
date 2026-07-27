import {createNavigationContainerRef} from '@react-navigation/native';
import type {RootStackParamList} from './types';

/** Permite navegar desde fuera de React (ej. api/client.ts avisando que se perdió la sesión). */
export const navigationRef = createNavigationContainerRef<RootStackParamList>();
