# ANIKA PDF — Release Checklist

Do not label the app as production-ready until these checks pass.

## Build
- [ ] Open project in Android Studio.
- [ ] Install required Android SDK and accept licenses.
- [ ] Sync Gradle successfully.
- [ ] Build debug APK.
- [ ] Install APK on a physical Android phone.
- [ ] Resolve all compile/runtime errors.

## Functional testing
- [ ] Image-to-PDF with multiple images.
- [ ] Open PDFs of different sizes and page counts.
- [ ] Navigate and preview pages.
- [ ] Hindi and English OCR on clear scans.
- [ ] Full-document OCR and TXT export.
- [ ] OCR text-to-PDF export and inspect Hindi glyph rendering.
- [ ] Export one/all pages to PNG.
- [ ] Merge, split, and rasterized compression.
- [ ] Add text overlay and verify original PDF remains unchanged.
- [ ] Test large PDFs for memory pressure and crashes.

## Before public release
- [ ] Verify privacy and permission declarations.
- [ ] Review all third-party library licenses.
- [ ] Add automated tests and crash reporting if appropriate.
- [ ] Test password-protected, malformed, and unusually large PDFs.
- [ ] Verify output quality and text search/selectability.
- [ ] Build signed release APK/AAB.
