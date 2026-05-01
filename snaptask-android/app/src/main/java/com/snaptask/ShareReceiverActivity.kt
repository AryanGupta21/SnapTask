package com.snaptask

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.snaptask.ocr.MLKitOCRProcessor
import com.snaptask.ui.theme.SnapTaskTheme
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    private val ocrProcessor = MLKitOCRProcessor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri = extractSharedImageUri(intent)
        val ocrState = mutableStateOf<OcrState>(
            if (imageUri == null) OcrState.Error("No image received") else OcrState.Loading
        )

        setContent {
            SnapTaskTheme {
                val state by ocrState
                OcrResultScreen(state = state)
            }
        }

        if (imageUri != null) {
            lifecycleScope.launch {
                ocrState.value = runCatching {
                    ocrProcessor.process(this@ShareReceiverActivity, imageUri).trim()
                }.fold(
                    onSuccess = { text ->
                        if (text.isBlank()) OcrState.Success("No text found") else OcrState.Success(text)
                    },
                    onFailure = { OcrState.Error("Could not read text from image") }
                )
            }
        }
    }

    internal fun extractSharedImageUri(intent: Intent?): Uri? {
        val i = intent?.takeIf { it.action == Intent.ACTION_SEND } ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            i.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}

@Composable
private fun OcrResultScreen(state: OcrState) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (state) {
            OcrState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Reading image...")
            }

            is OcrState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = state.message)
            }

            is OcrState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Extracted text")
                Text(text = state.text)
            }
        }
    }
}

private sealed interface OcrState {
    data object Loading : OcrState
    data class Success(val text: String) : OcrState
    data class Error(val message: String) : OcrState
}
