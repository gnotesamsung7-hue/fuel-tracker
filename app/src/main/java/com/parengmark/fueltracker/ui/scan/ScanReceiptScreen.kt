package com.parengmark.fueltracker.ui.scan

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.parengmark.fueltracker.data.receipt.ParsedReceipt
import com.parengmark.fueltracker.data.receipt.ReceiptParser
import java.io.File
import java.io.IOException

private enum class ScanState { IDLE, PROCESSING, DONE, ERROR }

private fun createReceiptImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "receipt-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun runOcr(
    context: Context,
    uri: Uri,
    onResult: (String) -> Unit,
    onError: () -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText -> onResult(visionText.text) }
            .addOnFailureListener { onError() }
    } catch (e: IOException) {
        onError()
    }
}

/**
 * Snaps a photo via the system camera app (no embedded camera preview, no
 * CAMERA permission needed -- see the manifest comment), runs it through
 * on-device ML Kit text recognition, and shows the best-effort parsed values
 * for the user to accept before they land in the (still fully editable)
 * add-fill-up form. Never auto-saves anything itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onUseValues: (ParsedReceipt) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var state by remember { mutableStateOf(ScanState.IDLE) }
    var parsed by remember { mutableStateOf<ParsedReceipt?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = photoUri
        if (success && uri != null) {
            state = ScanState.PROCESSING
            runOcr(
                context = context,
                uri = uri,
                onResult = { text ->
                    parsed = ReceiptParser.parse(text)
                    state = ScanState.DONE
                },
                onError = { state = ScanState.ERROR }
            )
        } else {
            state = ScanState.IDLE
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state) {
                ScanState.IDLE -> {
                    Text(
                        "Take a photo of your receipt. Recognition happens on your " +
                            "device -- the photo is never uploaded anywhere.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            val uri = createReceiptImageUri(context)
                            photoUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Text("  Take photo")
                    }
                }

                ScanState.PROCESSING -> {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text("Reading receipt…", modifier = Modifier.padding(top = 8.dp))
                    }
                }

                ScanState.DONE -> {
                    val p = parsed
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Here's what I could read off the receipt:", style = MaterialTheme.typography.bodyMedium)
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                            Text("Total cost: ${p?.totalCost ?: "not found"}")
                            Text("Volume: ${p?.volume ?: "not found"}")
                            Text("Price per unit: ${p?.pricePerUnit ?: "not found"}")
                            if (!p?.stationGuess.isNullOrBlank()) {
                                Text("Station (guess): ${p?.stationGuess}")
                            }
                            Text(
                                "Double-check these on the next screen -- OCR isn't perfect.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Button(
                        onClick = { parsed?.let(onUseValues) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Use these values") }
                    OutlinedButton(
                        onClick = { state = ScanState.IDLE; parsed = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Retake photo") }
                }

                ScanState.ERROR -> {
                    Text(
                        "Couldn't read that receipt clearly. You can retry, or just " +
                            "enter the fill-up manually.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { state = ScanState.IDLE },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Retry") }
                }
            }

            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip, enter manually")
            }
        }
    }
}
