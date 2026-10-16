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
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Open a PDF to start editing.") }
    var busy by remember { mutableStateOf(false) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var pagePreview by remember { mutableStateOf<Bitmap?>(null) }
    var addText by remember { mutableStateOf("") }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPdf = uri
            busy = true
            status = "Opening your PDF…"
            scope.launch {
                try {
                    pdfPageCount = withContext(Dispatchers.IO) { getPdfPageCount(context, uri) }
                    pageIndex = 0
                    status = "PDF opened • $pdfPageCount page(s)"
                } catch (e: Exception) {
                    selectedPdf = null
                    pdfPageCount = 0
                    status = "Could not open PDF: ${e.localizedMessage ?: "Unknown error"}"
                } finally { busy = false }
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { pagePreview?.let { if (!it.isRecycled) it.recycle() } }
    }

    LaunchedEffect(selectedPdf, pageIndex) {
        val uri = selectedPdf
        if (uri != null && pdfPageCount > 0) {
            val preview = withContext(Dispatchers.IO) {
                runCatching { renderPdfPage(context, uri, pageIndex) }.getOrNull()
            }
            pagePreview?.let { if (!it.isRecycled) it.recycle() }
            pagePreview = preview
        } else {
            pagePreview?.let { if (!it.isRecycled) it.recycle() }
            pagePreview = null
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Violet, secondary = Accent, background = Pale, surface = Color.White)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            Box(
                                Modifier.size(42.dp).background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(Violet, Accent)),
                                    RoundedCornerShape(14.dp)
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                            }
                            Column {
                                Text("ANIKA PDF", fontWeight = FontWeight.ExtraBold, color = Ink)
                                Text("PDF EDITOR", style = MaterialTheme.typography.labelSmall, color = Violet, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Pale
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Violet),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Your PDF. Your workspace.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "Open, review and edit documents in one clean workspace.",
                            color = Color(0xFFE7ECFF),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { pdfPicker.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Violet)
                        ) { Text(if (selectedPdf == null) "＋  Open PDF document" else "Open another PDF", fontWeight = FontWeight.Bold) }
                    }
                }

                if (selectedPdf == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(76.dp).background(Color(0xFFE9EEFF), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Description, contentDescription = null, tint = Violet, modifier = Modifier.size(38.dp)) }
                            Spacer(Modifier.height(16.dp))
                            Text("Ready to edit?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                            Spacer(Modifier.height(6.dp))
                            Text("Choose a PDF from your phone to open the editor.", color = Color(0xFF687386))
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { pdfPicker.launch("application/pdf") },
                                shape = RoundedCornerShape(13.dp)
                            ) { Text("Browse PDF files") }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("DOCUMENT PREVIEW", style = MaterialTheme.typography.labelMedium, color = Violet, fontWeight = FontWeight.Bold)
                                    Text("Page ${pageIndex + 1} of $pdfPageCount", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                                }
                                Surface(color = Color(0xFFE6F7F4), shape = RoundedCornerShape(10.dp)) {
                                    Text("PDF OPEN", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFF087F74), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 520.dp)
                                    .background(Color(0xFFF0F2F7), RoundedCornerShape(14.dp)).padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                pagePreview?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "PDF page preview",
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 490.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } ?: CircularProgressIndicator(color = Violet)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { if (pageIndex > 0) pageIndex-- },
                                    enabled = pageIndex > 0 && !busy,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("‹  Previous") }
                                Button(
                                    onClick = { if (pageIndex < pdfPageCount - 1) pageIndex++ },
                                    enabled = pageIndex < pdfPageCount - 1 && !busy,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Next  ›") }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("EDIT PDF", style = MaterialTheme.typography.labelMedium, color = Violet, fontWeight = FontWeight.Bold)
                            Text("Replace existing text", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Find printed text on the current page and replace it. Hindi and English are supported; review the saved copy carefully.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF687386)
                            )
                            OutlinedTextField(
                                value = findText, onValueChange = { findText = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Text to find") }, singleLine = true,
                                shape = RoundedCornerShape(13.dp)
                            )
                            OutlinedTextField(
                                value = replaceText, onValueChange = { replaceText = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Replace with") },
                                shape = RoundedCornerShape(13.dp)
                            )
                            Button(
                                onClick = {
                                    val uri = selectedPdf!!
                                    val old = findText.trim()
                                    val replacementText = replaceText
                                    if (old.isBlank() || replacementText.isBlank()) {
                                        status = "Enter the text to find and its replacement."
                                    } else {
                                        busy = true
                                        status = "Editing PDF page…"
                                        scope.launch {
                                            try {
                                                status = withContext(Dispatchers.IO) {
                                                    context.replaceTextByOcr(uri, pageIndex, old, replacementText)
                                                }
                                            } catch (e: Exception) {
                                                status = "Could not replace text: ${e.localizedMessage ?: "Unknown error"}"
                                            } finally { busy = false }
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(13.dp)
                            ) { Text("Replace text & save copy", fontWeight = FontWeight.Bold) }

                            HorizontalDivider(color = Color(0xFFE8EBF2))
                            Text("Add text to page", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = addText, onValueChange = { addText = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Type Hindi or English text") },
                                minLines = 2, shape = RoundedCornerShape(13.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    val uri = selectedPdf!!
                                    val textToAdd = addText.trim()
                                    if (textToAdd.isBlank()) {
                                        status = "Type the text you want to add."
                                    } else {
                                        busy = true
                                        status = "Adding text to a copy…"
                                        scope.launch {
                                            try {
                                                val name = withContext(Dispatchers.IO) {
                                                    context.exportPdfWithTextOverlay(uri, pageIndex, textToAdd)
                                                }
                                                status = "Saved to Downloads/$name"
                                            } catch (e: Exception) {
                                                status = "Could not add text: ${e.localizedMessage ?: "Unknown error"}"
                                            } finally { busy = false }
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(13.dp)
                            ) { Text("Add text & save copy") }
                            Text(
                                "Note: text replacement and added text are image-based edits. Original font, selectable text and complex layouts may not be preserved exactly.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A8494)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Accent)
                            Text("Editor status", fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(status, color = Ink)
                        if (busy) {
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                }
                Text(
                    "ANIKA PDF • A focused PDF editing workspace",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A93A3)
                )
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


/**
 * Best-effort visual replacement: OCR locates a matching text line, masks that area,
 * then draws the replacement and exports an image-based copy. It does not alter native
 * PDF text objects and cannot guarantee the original font or complex background.
 */
private suspend fun Context.replaceTextByOcr(uri: Uri, targetPageIndex: Int, oldText: String, newText: String): String = withContext(Dispatchers.IO) {
    val output = PdfDocument()
    try {
        val pfd = contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open PDF.")
        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                require(targetPageIndex in 0 until renderer.pageCount) { "Selected page is out of range." }
                var found = false
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { sourcePage ->
                        val scale = minOf(2.5f, 2000f / sourcePage.width.coerceAtLeast(1))
                        val bitmap = Bitmap.createBitmap((sourcePage.width * scale).toInt().coerceAtLeast(1), (sourcePage.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        try {
                            bitmap.eraseColor(AndroidColor.WHITE)
                            sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            if (index == targetPageIndex) {
                                val input = InputImage.fromBitmap(bitmap, 0)
                                val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                                val blocks = try {
                                    val a = runCatching { latin.process(input).await().textBlocks }.getOrDefault(emptyList())
                                    val b = runCatching { devanagari.process(input).await().textBlocks }.getOrDefault(emptyList())
                                    (a + b).distinctBy { it.text to it.boundingBox?.toShortString() }
                                } finally { latin.close(); devanagari.close() }
                                val match = blocks.firstOrNull { block ->
                                    block.boundingBox != null && block.text.contains(oldText, ignoreCase = true)
                                } ?: error("Text not found by OCR on this page. Try a shorter, clearly visible phrase.")
                                val rect = android.graphics.RectF(match.boundingBox!!)
                                val canvas = Canvas(bitmap)
                                val pad = (rect.height() * 0.12f).coerceAtLeast(2f)
                                rect.inset(-pad, -pad)
                                canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE; style = Paint.Style.FILL })
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.BLACK
                                    textSize = (rect.height() * 0.78f).coerceIn(12f, 96f)
                                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                                }
                                val maxWidth = rect.width()
                                var size = paint.textSize
                                while (paint.measureText(newText) > maxWidth && size > 8f) { size -= 1f; paint.textSize = size }
                                val x = rect.left + pad
                                val y = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
                                canvas.drawText(newText, x, y, paint)
                                found = true
                            }
                            val page = output.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                            val fit = minOf(595f / bitmap.width, 842f / bitmap.height)
                            val w = bitmap.width * fit; val h = bitmap.height * fit
                            val dst = RectF((595f - w) / 2f, (842f - h) / 2f, (595f + w) / 2f, (842f + h) / 2f)
                            page.canvas.drawColor(AndroidColor.WHITE)
                            page.canvas.drawBitmap(bitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                            output.finishPage(page)
                        } finally { if (!bitmap.isRecycled) bitmap.recycle() }
                    }
                }
                if (!found) error("No matching text was replaced.")
            }
        }
        val name = "ANIKA_TEXT_REPLACED_${timestamp()}.pdf"
        savePdfDocument(output, name)
        "Replacement saved to Downloads/$name. Note: this is an image-based copy; original font/layout are only approximated."
    } finally { output.close() }
}
