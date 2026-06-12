module.exports = {
  root: true,
  env: {
    es6: true,
    node: true,
  },
  extends: [
    "eslint:recommended",
    "plugin:import/errors",
    "plugin:import/warnings",
    "plugin:import/typescript",
    "google",
    "plugin:@typescript-eslint/recommended",
  ],
  parser: "@typescript-eslint/parser",
  parserOptions: {
    project: ["tsconfig.json", "tsconfig.dev.json"],
    sourceType: "module",
  },
  ignorePatterns: [
    "/lib/**/*", // Ignore built files.
  ],
  plugins: [
    "@typescript-eslint",
    "import",
  ],
  rules: {
    "quotes": ["error", "double"],
    "import/no-unresolved": 0,
    "indent": 0,
    "object-curly-spacing": ["error", "always"],
    "max-len": 0,
    "linebreak-style": 0,
    "quote-props": 0,
    "comma-dangle": 0,
    "no-trailing-spaces": 0,
    "brace-style": 0,
    "operator-linebreak": 0,
    "padded-blocks": 0,
    "arrow-parens": 0,
    "require-jsdoc": 0,
    "valid-jsdoc": 0,
    "block-spacing": 0,
    "@typescript-eslint/no-unused-vars": ["warn"],
  },
};
