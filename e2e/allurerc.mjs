import { defineConfig } from 'allure';

export default defineConfig({
  name: 'Databucket E2E',
  output: './allure-report',
  plugins: {
    awesome: {
      options: {
        // one index.html with everything inside: opens from the disk, e.g. downloaded from the GitHub Actions artifact
        singleFile: true,
        reportLanguage: 'en',
        groupBy: ['parentSuite', 'subSuite'],
      },
    },
  },
});
