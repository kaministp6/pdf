# How to obtain the ANIKA PDF APK

This source package includes a GitHub Actions workflow that attempts to build a debug APK on GitHub-hosted Android build runners. It has not been run from this environment, so a successful build is not guaranteed until the workflow completes.

## Steps
1. Sign in to GitHub and create a new repository.
2. Extract this ZIP on your computer.
3. Upload/push the contents of the `ANIKA_PDF_Android_Project` folder to the repository's `main` branch.
4. Open the repository's **Actions** tab.
5. Select **Build ANIKA PDF APK** and click **Run workflow** (or push to `main`).
6. Wait for the workflow. If it succeeds, open the completed run and download the **ANIKA-PDF-debug-APK** artifact.
7. Extract the downloaded artifact ZIP to get `app-debug.apk`.

If the workflow fails, open the run logs and fix the reported source/dependency/build errors. The debug APK is for testing, not a signed production release.

## Important
The build workflow does not magically implement planned features. This app is still an MVP: direct replacement of existing PDF text, full DOCX/XLSX conversion, handwriting recognition, and AI background repair are not fully implemented.
