module.exports = {
  env: {
    browser: true
  },
  plugins: ["@typescript-eslint"],
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/eslint-recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:@typescript-eslint/recommended-requiring-type-checking",
    "plugin:react/recommended",
  ],
  parser: "@typescript-eslint/parser",
  parserOptions: {
    ecmaFeatures: {
      jsx: true
    },
    project: "./tsconfig.eslint.json"
  },
  rules: {
    "react/no-unknown-property": ["error", { ignore: ["class"] }],
    "react/jsx-key": "off",
    "indent": ["error", "tab"]
  },
  settings: {
    react: {
      pragma: "h",
      version: "detect"
    },
  },
};