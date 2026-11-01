// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
// Zet alle opmaakregels uit die met Prettier botsen; opmaak is het werk van Prettier.
const prettier = require('eslint-config-prettier/flat');

module.exports = defineConfig([
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      // Templates en styles in aparte .html- en .scss-bestanden naast de .ts (F-33).
      '@angular-eslint/component-max-inline-declarations': [
        'error',
        { template: 0, styles: 0, animations: 0 },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
    },
  },
  {
    // Test-stubs (nep-componenten) mogen een inline template hebben.
    files: ['**/*.spec.ts'],
    rules: {
      '@angular-eslint/component-max-inline-declarations': 'off',
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },
  prettier,
]);
