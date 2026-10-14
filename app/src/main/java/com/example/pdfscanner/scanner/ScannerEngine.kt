package com.example.pdfscanner.scanner

import android.app.Activity
import android.content.Context
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning

object ScannerEngine {

    /**
     * Builds and configures the ML Kit Document Scanner.
     * Features: Edge detection, auto-crop, shadows removal, and PDF output.
     */
    fun getScanner(context: Context): GmsDocumentScanner {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        return GmsDocumentScanning.getClient(options)
    }

    /**
     * Starts the scanner activity.
     */
    fun startScan(
        activity: Activity,
        onIntentReady: (IntentSenderRequest) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val scanner = getScanner(activity)
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                val request = IntentSenderRequest.Builder(intentSender).build()
                onIntentReady(request)
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }
}
