package com.example.pdfscanner

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pdfscanner.pdf.PdfViewerScreen
import com.example.pdfscanner.scanner.ScannerEngine
import com.example.pdfscanner.signature.SignaturePad
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainAppScreen(activity = this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(activity: Activity) {
    var selectedPdf by remember { mutableStateOf<File?>(null) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var pdfList by remember { mutableStateOf(listOf<File>()) }

    // Refresh PDF documents from app internal storage
    val refreshPdfs = {
        val docsDir = activity.filesDir
        val files = docsDir.listFiles { _, name -> name.endsWith(".pdf") }?.toList() ?: emptyList()
        pdfList = files.sortedByDescending { it.lastModified() }
    }

    LaunchedEffect(Unit) {
        refreshPdfs()
    }

    // Activity launcher for ML Kit Document Scanner
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                val targetFile = File(activity.filesDir, "Scan_${System.currentTimeMillis()}.pdf")
                activity.contentResolver.openInputStream(pdf.uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(activity, "Document Scanned & Saved as PDF!", Toast.LENGTH_SHORT).show()
                refreshPdfs()
                selectedPdf = targetFile
            }
        }
    }

    if (selectedPdf != null) {
        PdfViewerScreen(
            pdfFile = selectedPdf!!,
            onBack = { selectedPdf = null }
        )
    } else if (showSignaturePad) {
        SignaturePad(
            onSaveSignature = { bitmap ->
                val sigFile = File(activity.filesDir, "signature_${System.currentTimeMillis()}.png")
                FileOutputStream(sigFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(activity, "Signature saved!", Toast.LENGTH_SHORT).show()
                showSignaturePad = false
            },
            onCancel = { showSignaturePad = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pro PDF Scanner & Reader") },
                    actions = {
                        IconButton(onClick = { showSignaturePad = true }) {
                            Icon(Icons.Default.Draw, contentDescription = "E-Signature")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        ScannerEngine.startScan(
                            activity = activity,
                            onIntentReady = { request -> scannerLauncher.launch(request) },
                            onError = { err ->
                                Toast.makeText(activity, "Scanner error: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    text = { Text("Scan Document") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (pdfList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No PDF documents yet.\nTap 'Scan Document' to start!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pdfList) { file ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPdf = file },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(text = file.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${file.length() / 1024} KB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
