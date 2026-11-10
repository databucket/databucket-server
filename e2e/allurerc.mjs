import { defineConfig } from 'allure';

export default defineConfig({
  name: 'Databucket E2E',
  output: './allure-report',
  plugins: {
    // the classic Allure 2 report: menu on the left, the tree of tests and the selected test side by side
    allure2: {
      options: {
        // one index.html with everything inside: opens from the disk, e.g. downloaded from the GitHub Actions artifact
        singleFile: true,
        reportLanguage: 'en',
      },
    },
  },
});
