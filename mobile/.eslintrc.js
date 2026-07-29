module.exports = {
  root: true,
  extends: '@react-native',
  overrides: [
    {
      // jest.setup.js y los tests corren en el entorno de jest: sin esto,
      // eslint marca `jest`, `describe`, etc. como no definidos (no-undef).
      files: ['jest.setup.js', '**/__tests__/**', '**/*.test.{js,jsx,ts,tsx}'],
      env: {jest: true},
    },
  ],
};
