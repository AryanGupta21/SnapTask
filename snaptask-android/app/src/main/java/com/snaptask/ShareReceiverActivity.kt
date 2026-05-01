package com.snaptask

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snaptask.ui.theme.SnapTaskTheme

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri = extractSharedImageUri(intent)

        setContent {
            SnapTaskTheme {
                ShareReceivedScreen(hasImage = imageUri != null)
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
private fun ShareReceivedScreen(hasImage: Boolean) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = if (hasImage) "Image received" else "No image received")
        }
    }
}
