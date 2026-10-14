# ANIKA PDF — Final Product Specification

## Product goal
A single Android application combining Basic, Advanced, and Professional PDF/document tools. This specification is the final target; it does not mean every feature is already implemented.

## Current source-project features
- Select multiple images and create a multi-page PDF.
- Open a PDF and navigate between pages.
- Preview the selected rendered page.
- OCR image or PDF pages with Latin and Devanagari ML Kit recognizers.
- Review/edit recognized OCR text, copy it, and export it to TXT.
- OCR every page of a PDF and export combined text.
- Export selected/all PDF pages as PNG images.
- Merge selected PDFs into one PDF.
- Create a rasterized smaller PDF copy.
- Split a PDF into one-page PDF files.
- Export OCR text as a PDF.
- Add new text overlay to the selected PDF page and save a separate copy.

## Final target features
### PDF editing
- Replace/edit existing text objects where technically possible.
- Add, remove, reposition, and style text.
- Add images, signatures, stamps, highlights, annotations, watermarks, headers, footers, and page numbers.
- Preserve original layout, fonts, selectable text, and vector content where possible.
- Page insert/delete/reorder/rotate/extract/duplicate.
- Merge, split, compress, password protection, and PDF information.

### OCR and documents
- Hindi, English, mixed-language OCR.
- Searchable PDF output.
- Handwriting recognition where supported by the chosen model.
- PDF/DOCX/XLSX/TXT conversions where supported by licensed libraries or conversion engines.
- OCR review and correction before export.

### Image and AI tools
- Image-to-PDF and PDF-to-image.
- Crop, rotate, brightness/contrast, scan cleanup.
- Optional AI image repair/background tools, subject to model/service availability.

### Security and quality
- Keep the original file unchanged by default; save edited copies.
- Minimize unnecessary uploads of sensitive documents.
- Background processing, progress, cancellation/error messaging, and memory management.
- Test on actual Android devices and a range of real-world PDFs before release.

## Technical honesty / limitations
- Font recognition and exact layout preservation cannot be guaranteed for every PDF.
- Scanned PDFs contain images rather than editable text; OCR is required first and may be inaccurate.
- The current merge/split/compression and text-overlay flows rasterize pages, which can remove selectable/searchable text, vector content, accessibility data, and original font objects.
- DOCX/XLSX conversion, handwriting recognition, AI repair, and true in-place editing of existing PDF text are not fully implemented in this source project.
- This archive is source code, not a built or verified APK.
