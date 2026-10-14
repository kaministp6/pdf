package com.anika.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF111827)
private val Violet = Color(0xFF3157D5)
private val Pale = Color(0xFFF3F6FB)
private val Accent = Color(0xFF13A89E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnikaPdfApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnikaPdfApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedPdfs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Welcome to ANIKA PDF.") }
    var busy by remember { mutableStateOf(false) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var pageToOcr by remember { mutableIntStateOf(0) }
    var outputImageUri by remember { mutableStateOf<Uri?>(null) }
    var pagePreview by remember { mutableStateOf<Bitmap?>(null) }
    var overlayText by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) { selectedImages = uris; status = "${uris.size} image(s) selected." }
    }
    val singleImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            busy = true; status = "Recognizing image text…"
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri).use { input ->
                            requireNotNull(input) { "Could not open image." }
                            BitmapFactory.decodeStream(input)
                        }
                    } ?: error("Unsupported or damaged image.")
                    ocrText = recognizeText(bitmap)
                    bitmap.recycle()
                    status = if (ocrText.isBlank()) "No text detected. Try a clearer image." else "OCR complete. Review the result below."
                } catch (e: Exception) { status = "OCR failed: ${e.localizedMessage ?: "unknown error"}" }
                finally { busy = false }
            }
        }
    }
    val multiPdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedPdfs = uris
            status = "${uris.size} PDF file(s) selected for merging."
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPdf = uri; busy = true; status = "Reading PDF…"
            scope.launch {
                try {
                    pdfPageCount = withContext(Dispatchers.IO) { getPdfPageCount(context, uri) }
                    pageToOcr = 0
                    status = "PDF opened: $pdfPageCount page(s)."
                } catch (e: Exception) { selectedPdf = null; pdfPageCount = 0; status = "Could not open PDF: ${e.localizedMessage ?: "unknown error"}" }
                finally { busy = false }
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            pagePreview?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Violet, secondary = Accent, background = Pale, surface = Color.White)) {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("ANIKA PDF", fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("PDF • EDIT • CONVERT", style = MaterialTheme.typography.labelSmall, color = Violet, fontWeight = FontWeight.Bold)
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White))
            },
            containerColor = Pale
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Violet), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Your documents.\nUnder your control.", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Create, organize and convert PDFs with Hindi & English OCR.", color = Color(0xFFE8ECFF), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { pdfPicker.launch("application/pdf") }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Violet), shape = RoundedCornerShape(14.dp)) { Text("＋ Open PDF", fontWeight = FontWeight.Bold) }
                            OutlinedButton(onClick = { imagePicker.launch("image/*") }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), shape = RoundedCornerShape(14.dp)) { Text("Images → PDF") }
                        }
                    }
                }
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).background(Color(0xFFE6F7F4), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Accent)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(if (selectedPdf != null) "PDF ready to work" else "Quick workspace", fontWeight = FontWeight.Bold, color = Ink)
                            Text(if (selectedPdf != null) "$pdfPageCount pages • Choose a tool below" else "Open a PDF or choose images to get started", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text("Create & scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                ToolCard("Images → PDF", if (selectedImages.isEmpty()) "Select one or more images to create a multi-page PDF." else "${selectedImages.size} image(s) selected.",
                    { Icon(Icons.Default.Image, null, tint = Violet) }, "Choose images", !busy) { imagePicker.launch("image/*") }
                ToolCard("Create PDF", "Put each selected image on its own A4-sized page.",
                    { Icon(Icons.Default.Description, null, tint = Violet) }, "Create PDF", selectedImages.isNotEmpty() && !busy) {
                    busy = true; status = "Creating PDF…"
                    scope.launch {
                        try {
                            val name = withContext(Dispatchers.IO) { context.createPdfFromImages(selectedImages) }
                            status = "PDF saved to Downloads/$name"
                        } catch (e: Exception) { status = "PDF creation failed: ${e.localizedMessage ?: "unknown error"}" }
                        finally { busy = false }
                    }
                }
                ToolCard("OCR from image", "Recognize Hindi/Devanagari and English printed text and some handwriting.",
                    { Icon(Icons.Default.TextFields, null, tint = Violet) }, "Select image", !busy) { singleImagePicker.launch("image/*") }

                Text("PDF tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                ToolCard("Merge PDFs", if (selectedPdfs.isEmpty()) "Choose multiple PDFs to combine them into one file." else "${selectedPdfs.size} PDF file(s) selected.",
                    { Icon(Icons.Default.Description, null, tint = Violet) }, "Choose PDFs", !busy) { multiPdfPicker.launch("application/pdf") }
                ToolCard("Create merged PDF", "Combine selected PDFs in order. Pages are rasterized, so the merged file may not retain selectable text or original vectors.",
                    { Icon(Icons.Default.AutoAwesome, null, tint = Violet) }, "Merge selected PDFs", selectedPdfs.size >= 2 && !busy) {
                    busy = true; status = "Merging PDFs…"
                    scope.launch {
                        try {
                            val name = withContext(Dispatchers.IO) { context.mergePdfsAsPages(selectedPdfs) }
                            status = "Merged PDF saved to Downloads/$name"
                            selectedPdfs = emptyList()
                        } catch (e: Exception) { status = "Merge failed: ${e.localizedMessage ?: "unknown error"}" }
                        finally { busy = false }
                    }
                }
                ToolCard("Compress PDF (rasterized)", "Create a smaller image-based copy of the selected PDF. Text may no longer be selectable; keep the original.",
                    { Icon(Icons.Default.Description, null, tint = Violet) }, "Compress selected PDF", selectedPdf != null && !busy) {
                    val uri = selectedPdf
                    if (uri != null) {
                        busy = true; status = "Creating compressed copy…"
                        scope.launch {
                            try {
                                val name = withContext(Dispatchers.IO) { context.rasterizePdf(uri, 1.25f, "ANIKA_COMPRESSED") }
                                status = "Compressed copy saved to Downloads/$name"
                            } catch (e: Exception) { status = "Compression failed: ${e.localizedMessage ?: "unknown error"}" }
                            finally { busy = false }
                        }
                    }
                }
                ToolCard("Split PDF into pages", "Save each page of the selected PDF as a separate one-page PDF file. Output pages are image-based.",
                    { Icon(Icons.Default.Description, null, tint = Violet) }, "Split into separate PDFs", selectedPdf != null && !busy) {
                    val uri = selectedPdf
                    if (uri != null) {
                        busy = true; status = "Splitting PDF into separate pages…"
                        scope.launch {
                            try {
                                val count = withContext(Dispatchers.IO) { context.splitPdfIntoPages(uri) }
                                status = "Split complete: $count one-page PDF file(s) saved to Downloads."
                            } catch (e: Exception) { status = "Split failed: ${e.localizedMessage ?: "unknown error"}" }
                            finally { busy = false }
                        }
                    }
                }
                ToolCard("Open PDF", if (selectedPdf == null) "Open a PDF and see how many pages it contains." else "Opened PDF: $pdfPageCount page(s).",
                    { Icon(Icons.Default.Description, null, tint = Violet) }, "Choose PDF", !busy) { pdfPicker.launch("application/pdf") }

                if (selectedPdf != null && pdfPageCount > 0) {
                    LaunchedEffect(selectedPdf, pageToOcr) {
                        val uri = selectedPdf
                        if (uri != null) {
                            val preview = withContext(Dispatchers.IO) {
                                runCatching { renderPdfPage(context, uri, pageToOcr) }.getOrNull()
                            }
                            pagePreview?.let { if (!it.isRecycled) it.recycle() }
                            pagePreview = preview
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Document preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Ink)
                            Spacer(Modifier.height(8.dp))
                            pagePreview?.let { preview ->
                                Image(bitmap = preview.asImageBitmap(), contentDescription = "PDF page preview",
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp), contentScale = ContentScale.Fit)
                            } ?: Text("Preview loading…", color = Ink)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Page actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Page ${pageToOcr + 1} of $pdfPageCount")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { if (pageToOcr > 0) pageToOcr-- }, enabled = pageToOcr > 0 && !busy, modifier = Modifier.weight(1f)) { Text("Previous") }
                                OutlinedButton(onClick = { if (pageToOcr < pdfPageCount - 1) pageToOcr++ }, enabled = pageToOcr < pdfPageCount - 1 && !busy, modifier = Modifier.weight(1f)) { Text("Next") }
                            }
                            OutlinedTextField(
                                value = overlayText,
                                onValueChange = { overlayText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Text to add on this page") },
                                placeholder = { Text("Type Hindi or English text…") },
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(onClick = {
                                val uri = selectedPdf!!
                                val pageIndex = pageToOcr
                                val text = overlayText.trim()
                                if (text.isBlank()) {
                                    status = "Please enter text to add."
                                } else {
                                    busy = true; status = "Creating a copy with text overlay…"
                                    scope.launch {
                                        try {
                                            val name = withContext(Dispatchers.IO) {
                                                context.exportPdfWithTextOverlay(uri, pageIndex, text)
                                            }
                                            status = "Text-overlay copy saved to Downloads/$name. Original PDF unchanged."
                                        } catch (e: Exception) {
                                            status = "Text overlay failed: ${e.localizedMessage ?: "unknown error"}"
                                        } finally { busy = false }
                                    }
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                Text("Add text to this page and save a copy")
                            }
                            Text(
                                "Note: this adds new text over the selected page; it does not replace existing PDF text. The output is image-based.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF666477)
                            )
                            Button(onClick = {
                                val uri = selectedPdf!!; val index = pageToOcr
                                busy = true; status = "Rendering PDF page…"
                                scope.launch {
                                    try {
                                        val bitmap = withContext(Dispatchers.IO) { renderPdfPage(context, uri, index) }
                                        ocrText = recognizeText(bitmap); bitmap.recycle()
                                        status = "Page ${index + 1} OCR complete. Review and correct the text."
                                    } catch (e: Exception) { status = "OCR failed: ${e.localizedMessage ?: "unknown error"}" }
                                    finally { busy = false }
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Recognize text on this page") }
                            OutlinedButton(onClick = {
                                val uri = selectedPdf!!
                                val pageCount = pdfPageCount
                                busy = true; status = "Exporting all $pageCount pages as PNG…"
                                scope.launch {
                                    try {
                                        var saved = 0
                                        for (index in 0 until pageCount) {
                                            status = "Exporting page ${index + 1} of $pageCount…"
                                            val bitmap = withContext(Dispatchers.IO) { renderPdfPage(context, uri, index) }
                                            try {
                                                val fileName = withContext(Dispatchers.IO) {
                                                    context.saveBitmapAsPng(bitmap, "ANIKA_PAGE_${String.format(Locale.US, "%03d", index + 1)}_${timestamp()}.png")
                                                }
                                                saved++
                                            } finally {
                                                if (!bitmap.isRecycled) bitmap.recycle()
                                            }
                                        }
                                        status = "Export complete: $saved PNG file(s) saved to Downloads."
                                    } catch (e: Exception) {
                                        status = "Export all pages failed: ${e.localizedMessage ?: "unknown error"}"
                                    } finally { busy = false }
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Export all pages as PNG") }
                            Button(onClick = {
                                val uri = selectedPdf!!
                                val pageCount = pdfPageCount
                                busy = true; status = "Starting OCR for all $pageCount pages…"
                                scope.launch {
                                    try {
                                        val fullText = buildString {
                                            for (index in 0 until pageCount) {
                                                status = "Recognizing page ${index + 1} of $pageCount…"
                                                val bitmap = withContext(Dispatchers.IO) { renderPdfPage(context, uri, index) }
                                                val pageText = try { recognizeText(bitmap) } finally { if (!bitmap.isRecycled) bitmap.recycle() }
                                                appendLine("========== PAGE ${index + 1} ==========")
                                                appendLine(pageText.ifBlank { "[No text detected on this page]" })
                                                appendLine()
                                            }
                                        }
                                        ocrText = fullText
                                        val name = withContext(Dispatchers.IO) { context.saveTextFile(fullText) }
                                        status = "All $pageCount pages processed. Full OCR text saved to Downloads/$name"
                                    } catch (e: Exception) {
                                        status = "Full-document OCR failed: ${e.localizedMessage ?: "unknown error"}"
                                    } finally { busy = false }
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("OCR all pages and export TXT") }
                            Button(onClick = {
                                val uri = selectedPdf!!
                                busy = true; status = "Rendering current page as PNG…"
                                scope.launch {
                                    try {
                                        val bitmap = withContext(Dispatchers.IO) { renderPdfPage(context, uri, pageToOcr) }
                                        val name = withContext(Dispatchers.IO) { context.saveBitmapAsPng(bitmap) }
                                        bitmap.recycle()
                                        status = "Current page exported to Downloads/$name"
                                    } catch (e: Exception) { status = "Page export failed: ${e.localizedMessage ?: "unknown error"}" }
                                    finally { busy = false }
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors()) { Text("Export this page as PNG") }
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Violet); Spacer(Modifier.width(8.dp))
                            Text("Status", fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Spacer(Modifier.height(6.dp)); Text(status, color = Ink)
                        if (busy) { Spacer(Modifier.height(10.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    }
                }

                if (ocrText.isNotBlank()) {
                    Text("Recognized text — review before use", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                    OutlinedTextField(value = ocrText, onValueChange = { ocrText = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                        label = { Text("Editable OCR result") }, shape = RoundedCornerShape(16.dp))
                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ANIKA PDF OCR", ocrText)); status = "Text copied to clipboard."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Copy text") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            try { val name = withContext(Dispatchers.IO) { context.saveTextFile(ocrText) }; status = "Text saved to Downloads/$name" }
                            catch (e: Exception) { status = "Text export failed: ${e.localizedMessage ?: "unknown error"}" }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Export OCR as TXT") }
                    OutlinedButton(onClick = {
                        val textToExport = ocrText
                        busy = true
                        scope.launch {
                            try {
                                val name = withContext(Dispatchers.IO) { context.saveTextAsPdf(textToExport) }
                                status = "OCR text PDF saved to Downloads/$name"
                            } catch (e: Exception) {
                                status = "Text PDF export failed: ${e.localizedMessage ?: "unknown error"}"
                            } finally { busy = false }
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Export OCR text as PDF") }
                }

                Text("This is an MVP source project. Direct editing of existing PDF text, universal font recognition, DOCX/XLSX conversion, PDF merge/split/compress, and AI background repair are not implemented yet. OCR may make mistakes; always review results.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF666477))
            }
        }
    }
}

@Composable
private fun ToolCard(title: String, description: String, icon: @Composable () -> Unit, button: String, enabled: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Color(0xFFF0EEFF), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { icon() }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(4.dp)); Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF666477))
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(button) }
        }
    }
}

private suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
    val input = InputImage.fromBitmap(bitmap, 0)
    val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    try {
        val latinText = runCatching { latin.process(input).await().text }.getOrDefault("")
        val devanagariText = runCatching { devanagari.process(input).await().text }.getOrDefault("")
        listOf(devanagariText, latinText).filter { it.isNotBlank() }.distinct().joinToString("\n\n--- Alternative OCR result ---\n\n")
    } finally { latin.close(); devanagari.close() }
}

private fun getPdfPageCount(context: Context, uri: Uri): Int {
    val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open PDF.")
    pfd.use { descriptor -> PdfRenderer(descriptor).use { renderer -> return renderer.pageCount } }
}

private fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): Bitmap {
    val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open PDF.")
    pfd.use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            require(pageIndex in 0 until renderer.pageCount) { "Page index out of range." }
            renderer.openPage(pageIndex).use { page ->
                val scale = minOf(3f, 2200f / page.width.coerceAtLeast(1))
                val bitmap = Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1), (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(AndroidColor.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }
    }
}

private fun Context.createPdfFromImages(uris: List<Uri>): String {
    require(uris.isNotEmpty()) { "Please select at least one image." }
    val document = PdfDocument()
    try {
        uris.forEachIndexed { index, uri ->
            val bitmap = contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Could not open image." }; BitmapFactory.decodeStream(input)
            } ?: error("Could not decode image.")
            val pageWidth = 595; val pageHeight = 842
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
            val scale = minOf(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height)
            val width = bitmap.width * scale; val height = bitmap.height * scale
            val dest = RectF((pageWidth - width) / 2f, (pageHeight - height) / 2f, (pageWidth + width) / 2f, (pageHeight + height) / 2f)
            page.canvas.drawColor(AndroidColor.WHITE)
            page.canvas.drawBitmap(bitmap, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            document.finishPage(page); if (!bitmap.isRecycled) bitmap.recycle()
        }
        val fileName = "ANIKA_PDF_${timestamp()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName); put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val outputUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Could not create output file.")
        contentResolver.openOutputStream(outputUri).use { output -> requireNotNull(output) { "Could not open output stream." }; document.writeTo(output) }
        return fileName
    } finally { document.close() }
}

private fun Context.saveTextFile(text: String): String {
    val name = "ANIKA_OCR_${timestamp()}.txt"
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Could not create text file.")
    contentResolver.openOutputStream(uri).use { output -> requireNotNull(output) { "Could not open output stream." }; output.write(text.toByteArray(Charsets.UTF_8)) }
    return name
}

private fun Context.saveBitmapAsPng(bitmap: Bitmap, fileNameOverride: String? = null): String {
    val name = fileNameOverride ?: "ANIKA_PAGE_${timestamp()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "image/png")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Could not create PNG.")
    contentResolver.openOutputStream(uri).use { output ->
        requireNotNull(output) { "Could not open output stream." }
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) error("Could not encode PNG.")
    }
    return name
}


private fun Context.mergePdfsAsPages(uris: List<Uri>): String {
    require(uris.size >= 2) { "Select at least two PDF files." }
    val out = PdfDocument()
    try {
        var outputPage = 1
        for (uri in uris) {
            val pfd = contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open one of the PDFs.")
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    for (index in 0 until renderer.pageCount) {
                        renderer.openPage(index).use { sourcePage ->
                            val scale = minOf(1.8f, 1400f / sourcePage.width.coerceAtLeast(1))
                            val bitmap = Bitmap.createBitmap(
                                (sourcePage.width * scale).toInt().coerceAtLeast(1),
                                (sourcePage.height * scale).toInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.eraseColor(AndroidColor.WHITE)
                            sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val pageWidth = 595
                            val pageHeight = 842
                            val page = out.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, outputPage++).create())
                            val fit = minOf(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height)
                            val w = bitmap.width * fit
                            val h = bitmap.height * fit
                            val rect = RectF((pageWidth - w) / 2f, (pageHeight - h) / 2f, (pageWidth + w) / 2f, (pageHeight + h) / 2f)
                            page.canvas.drawColor(AndroidColor.WHITE)
                            page.canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                            out.finishPage(page)
                            bitmap.recycle()
                        }
                    }
                }
            }
        }
        val name = "ANIKA_MERGED_${timestamp()}.pdf"
        savePdfDocument(out, name)
        return name
    } finally {
        out.close()
    }
}

private fun Context.rasterizePdf(uri: Uri, scaleLimit: Float, prefix: String): String {
    val out = PdfDocument()
    try {
        val pfd = contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open PDF.")
        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { sourcePage ->
                        val scale = minOf(scaleLimit, 1800f / sourcePage.width.coerceAtLeast(1))
                        val bitmap = Bitmap.createBitmap(
                            (sourcePage.width * scale).toInt().coerceAtLeast(1),
                            (sourcePage.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.RGB_565
                        )
                        bitmap.eraseColor(AndroidColor.WHITE)
                        sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val pageWidth = 595
                        val pageHeight = 842
                        val page = out.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                        val fit = minOf(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height)
                        val w = bitmap.width * fit
                        val h = bitmap.height * fit
                        val rect = RectF((pageWidth - w) / 2f, (pageHeight - h) / 2f, (pageWidth + w) / 2f, (pageHeight + h) / 2f)
                        page.canvas.drawColor(AndroidColor.WHITE)
                        page.canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        out.finishPage(page)
                        bitmap.recycle()
                    }
                }
            }
        }
        val name = "${prefix}_${timestamp()}.pdf"
        savePdfDocument(out, name)
        return name
    } finally {
        out.close()
    }
}

private fun Context.savePdfDocument(document: PdfDocument, fileName: String) {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error("Could not create output PDF.")
    contentResolver.openOutputStream(uri).use { output ->
        requireNotNull(output) { "Could not open output stream." }
        document.writeTo(output)
    }
}

private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())


private fun Context.splitPdfIntoPages(uri: Uri): Int {
    var savedCount = 0
    val pfd = contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open PDF.")
    pfd.use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            require(renderer.pageCount > 0) { "This PDF has no pages." }
            for (index in 0 until renderer.pageCount) {
                val onePagePdf = PdfDocument()
                try {
                    renderer.openPage(index).use { sourcePage ->
                        val scale = minOf(1.8f, 1600f / sourcePage.width.coerceAtLeast(1))
                        val bitmap = Bitmap.createBitmap(
                            (sourcePage.width * scale).toInt().coerceAtLeast(1),
                            (sourcePage.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.RGB_565
                        )
                        try {
                            bitmap.eraseColor(AndroidColor.WHITE)
                            sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val pageWidth = 595
                            val pageHeight = 842
                            val page = onePagePdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
                            val fit = minOf(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height)
                            val w = bitmap.width * fit
                            val h = bitmap.height * fit
                            val rect = RectF((pageWidth - w) / 2f, (pageHeight - h) / 2f, (pageWidth + w) / 2f, (pageHeight + h) / 2f)
                            page.canvas.drawColor(AndroidColor.WHITE)
                            page.canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                            onePagePdf.finishPage(page)
                        } finally {
                            if (!bitmap.isRecycled) bitmap.recycle()
                        }
                    }
                    val name = "ANIKA_PAGE_${String.format(Locale.US, "%03d", index + 1)}_${timestamp()}.pdf"
                    savePdfDocument(onePagePdf, name)
                    savedCount++
                } finally {
                    onePagePdf.close()
                }
            }
        }
    }
    return savedCount
}


private fun Context.saveTextAsPdf(text: String): String {
    require(text.isNotBlank()) { "There is no OCR text to export." }
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 42f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        textSize = 12f
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }
    val maxWidth = pageWidth - margin * 2
    val lineHeight = 18f
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var canvas: Canvas? = null
    var y = 0f

    fun startNewPage() {
        page?.let { document.finishPage(it) }
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page!!.canvas
        canvas!!.drawColor(AndroidColor.WHITE)
        y = margin
    }

    fun drawWrappedLine(line: String) {
        if (page == null || y + lineHeight > pageHeight - margin) startNewPage()
        if (line.isEmpty()) {
            y += lineHeight
            return
        }
        var remaining = line
        while (remaining.isNotEmpty()) {
            var end = remaining.length
            while (end > 1 && paint.measureText(remaining.substring(0, end)) > maxWidth) end--
            if (end < remaining.length) {
                val space = remaining.lastIndexOf(' ', end)
                if (space > 0) end = space
            }
            val chunk = remaining.substring(0, end).trimEnd()
            if (y + lineHeight > pageHeight - margin) startNewPage()
            canvas!!.drawText(chunk, margin, y, paint)
            y += lineHeight
            remaining = remaining.substring(end).trimStart()
        }
    }

    try {
        text.split("\n").forEach { drawWrappedLine(it) }
        page?.let { document.finishPage(it) }
        val name = "ANIKA_OCR_${timestamp()}.pdf"
        savePdfDocument(document, name)
        return name
    } finally {
        document.close()
    }
}


private fun Context.exportPdfWithTextOverlay(uri: Uri, targetPageIndex: Int, overlayText: String): String {
    val output = PdfDocument()
    val pfd = contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open PDF.")
    try {
        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                require(targetPageIndex in 0 until renderer.pageCount) { "Selected page is out of range." }
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { sourcePage ->
                        val scale = minOf(2f, 1600f / sourcePage.width.coerceAtLeast(1))
                        val bitmap = Bitmap.createBitmap(
                            (sourcePage.width * scale).toInt().coerceAtLeast(1),
                            (sourcePage.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        try {
                            bitmap.eraseColor(AndroidColor.WHITE)
                            sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            if (index == targetPageIndex) {
                                val canvas = Canvas(bitmap)
                                val margin = (bitmap.width * 0.05f).coerceAtLeast(24f)
                                val boxHeight = (bitmap.height * 0.18f).coerceAtLeast(100f)
                                val box = RectF(margin, bitmap.height - boxHeight - margin, bitmap.width - margin, bitmap.height - margin)
                                val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.WHITE
                                    alpha = 235
                                    style = Paint.Style.FILL
                                }
                                canvas.drawRoundRect(box, 12f, 12f, backgroundPaint)
                                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.DKGRAY
                                    style = Paint.Style.STROKE
                                    strokeWidth = (bitmap.width * 0.002f).coerceAtLeast(2f)
                                }
                                canvas.drawRoundRect(box, 12f, 12f, borderPaint)
                                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.BLACK
                                    textSize = (bitmap.width * 0.035f).coerceIn(18f, 48f)
                                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                                }
                                val maxWidth = box.width() - margin
                                val lineHeight = textPaint.textSize * 1.35f
                                var y = box.top + textPaint.textSize + margin * 0.25f
                                overlayText.split("\n").forEach { originalLine ->
                                    var remaining = originalLine
                                    if (remaining.isEmpty()) {
                                        y += lineHeight
                                    } else {
                                        while (remaining.isNotEmpty() && y < box.bottom - margin * 0.2f) {
                                            var end = remaining.length
                                            while (end > 1 && textPaint.measureText(remaining.substring(0, end)) > maxWidth) end--
                                            if (end < remaining.length) {
                                                val space = remaining.lastIndexOf(' ', end)
                                                if (space > 0) end = space
                                            }
                                            val chunk = remaining.substring(0, end).trimEnd()
                                            canvas.drawText(chunk, box.left + margin * 0.25f, y, textPaint)
                                            y += lineHeight
                                            remaining = remaining.substring(end).trimStart()
                                        }
                                    }
                                    if (y >= box.bottom - margin * 0.2f) return@forEach
                                }
                            }
                            val pageWidth = 595
                            val pageHeight = 842
                            val page = output.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                            val fit = minOf(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height)
                            val w = bitmap.width * fit
                            val h = bitmap.height * fit
                            val rect = RectF((pageWidth - w) / 2f, (pageHeight - h) / 2f, (pageWidth + w) / 2f, (pageHeight + h) / 2f)
                            page.canvas.drawColor(AndroidColor.WHITE)
                            page.canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                            output.finishPage(page)
                        } finally {
                            if (!bitmap.isRecycled) bitmap.recycle()
                        }
                    }
                }
            }
        }
        val name = "ANIKA_TEXT_OVERLAY_${timestamp()}.pdf"
        savePdfDocument(output, name)
        return name
    } finally {
        output.close()
    }
}
