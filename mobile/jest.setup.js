// Mocks de modulos nativos: en jest no hay puente nativo, asi que cualquier
// libreria que lo toque (keychain, netinfo, SSE, screens) tiene que
// reemplazarse por un doble JS o el render del arbol falla al importarlas.

// react-native-keychain: usado por src/auth/secureStorage.ts para el refresh token.
jest.mock('react-native-keychain', () => ({
  setGenericPassword: jest.fn().mockResolvedValue(true),
  getGenericPassword: jest.fn().mockResolvedValue(false),
  resetGenericPassword: jest.fn().mockResolvedValue(true),
  ACCESSIBLE: {WHEN_UNLOCKED_THIS_DEVICE_ONLY: 'WhenUnlockedThisDeviceOnly'},
}));

// AsyncStorage: usado por ThemeProvider para cachear el branding del balneario.
jest.mock('@react-native-async-storage/async-storage', () => ({
  getItem: jest.fn().mockResolvedValue(null),
  setItem: jest.fn().mockResolvedValue(undefined),
  removeItem: jest.fn().mockResolvedValue(undefined),
}));

// NetInfo: useNetworkStatus se suscribe al estado de conectividad.
jest.mock('@react-native-community/netinfo', () => ({
  __esModule: true,
  default: {
    addEventListener: jest.fn(() => jest.fn()),
    fetch: jest.fn().mockResolvedValue({isConnected: true}),
  },
}));

// react-native-sse: el canal de tiempo real (ADR-003). No debe abrir una
// conexion real durante los tests.
jest.mock('react-native-sse', () => {
  return jest.fn().mockImplementation(() => ({
    addEventListener: jest.fn(),
    removeAllEventListeners: jest.fn(),
    close: jest.fn(),
  }));
});

jest.mock('react-native-screens', () => ({
  enableScreens: jest.fn(),
  enableFreeze: jest.fn(),
}));
