module.exports = {
  env: {
    browser: true
  },
  plugins: ['@typescript-eslint'],
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/eslint-recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:@typescript-eslint/recommended-requiring-type-checking",
    "plugin:react/recommended",
    "plugin:react-hooks/recommended",
    "plugin:prettier/recommended",
    "prettier/@typescript-eslint",
    "prettier/react",
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaFeatures: {
      jsx: true
    },
    project: './tsconfig.eslint.json',
  },
  rules: {
    'react/no-unknown-property': ['error', { ignore: ['class'] }],
    'react/jsx-key': 0,
    'no-console': 2,
    'no-alert': 2,
  },
  settings: {
    react: {
      pragma: 'h',
      version: 'detect'
    },
  }
};
