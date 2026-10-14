# ANIKA PDF — Android Source Project (Release Candidate 1.0.0)

## What this package is
This is an Android Studio source project for the current ANIKA PDF MVP. It is **not a prebuilt APK**. It has not been compiled or tested on a physical Android device in this environment.

## Current implemented features
- Multi-image to multi-page PDF
- Open PDF, page navigation, and rendered page preview
- OCR from images or PDF pages using ML Kit Latin and Devanagari recognizers
- Review/edit OCR output, copy text, export TXT
- OCR all pages and export combined text to TXT
- Export one/all PDF pages as PNG
- Merge multiple PDFs into one PDF
- Split a PDF into one-page PDFs
- Create a rasterized smaller PDF copy
- Export OCR text to PDF
- Add a text overlay to the selected page and save a separate PDF copy

## Important limitations
- Text overlay adds text on top; it does not replace existing PDF text.
- Merge, split, compression, and text-overlay workflows render pages as images. Output may lose selectable/searchable text, vector content, accessibility data, and original font objects.
- Direct in-place editing of existing PDF text, full DOCX/XLSX conversion, universal font recognition, handwriting recognition, and AI background repair are not implemented in this MVP.
- OCR may make mistakes; review output before relying on it.

## Build on Windows
1. Install Android Studio and the Android SDK required by the project (compile SDK 35).
2. Open the `ANIKA_PDF_Android_Project` folder in Android Studio.
3. Wait for Gradle sync and resolve any dependency/build errors.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Debug APK output should be `app/build/outputs/apk/debug/app-debug.apk`.
6. Install and test on a physical Android phone before using real documents.

Alternatively, if Gradle and Android SDK are configured on PATH, run `build_debug_apk_windows.bat` on Windows or `./build_debug_apk.sh` on Linux/macOS.

## Final product target
See `FINAL_PRODUCT_SPECIFICATION.md` for the complete planned feature set and `RELEASE_CHECKLIST.md` for test requirements. Planned features are not all implemented in this MVP.


## Automated APK build option
This project now includes `.github/workflows/build-apk.yml` and `APK_BUILD_GUIDE.md`. Upload the project to a GitHub repository and run the workflow to attempt a debug APK build. The workflow has not been run in this environment, so the APK must be downloaded only after a successful workflow run.
