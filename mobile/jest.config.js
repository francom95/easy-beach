// Los paquetes de RN se publican como ESM sin transpilar: jest ignora
// node_modules por defecto, asi que hay que whitelistear explicitamente los que
// la app importa o el require falla con "Unexpected token 'export'".
const esmPackages = [
  '@react-native',
  'react-native',
  '@react-navigation',
  'react-native-safe-area-context',
  'react-native-screens',
  'react-native-keychain',
  'react-native-sse',
  'react-native-svg',
  'react-native-vector-icons',
  '@react-native-async-storage',
  '@react-native-community',
];

module.exports = {
  preset: '@react-native/jest-preset',
  setupFiles: ['<rootDir>/jest.setup.js'],
  transformIgnorePatterns: [`node_modules/(?!(?:${esmPackages.join('|')})/)`],
};
