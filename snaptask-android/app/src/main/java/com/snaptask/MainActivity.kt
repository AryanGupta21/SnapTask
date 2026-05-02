package com.snaptask

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.snaptask.ui.theme.SnapTaskTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private var pendingCameraUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { launchPipeline(it) }
    }

    private val pickFromGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { launchPipeline(it) } }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) openCamera() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapTaskTheme {
                LauncherScreen(
                    onScanCamera = { launchCamera() },
                    onPickGallery = {
                        pickFromGallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val file = File(cacheDir, "camera/snap_${System.currentTimeMillis()}.jpg")
            .also { it.parentFile?.mkdirs() }
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        pendingCameraUri = uri
        takePicture.launch(uri)
    }

    private fun launchPipeline(uri: Uri) {
        startActivity(
            Intent(this, ShareReceiverActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }
}

@Composable
private fun LauncherScreen(
    onScanCamera: () -> Unit,
    onPickGallery: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A1A), Color(0xFF0D1B4B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Spacer(Modifier.weight(1f))

            // Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF1428A0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 40.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "SnapTask",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Point. Snap. Done.",
                fontSize = 16.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // Feature pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill("📅 Calendar")
                FeaturePill("👤 Contacts")
                FeaturePill("📝 Notes")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill("🧾 Expenses")
                FeaturePill("🤖 AI-powered")
            }

            Spacer(Modifier.weight(1f))

            // Primary CTA
            Button(
                onClick = onScanCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1428A0)
                )
            ) {
                Text(
                    text = "📷  Scan Image",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Secondary CTA
            TextButton(
                onClick = onPickGallery,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Or pick from gallery",
                    color = Color(0xFF8899CC),
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeaturePill(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF1A2040)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color(0xFFCCDDFF),
            fontSize = 13.sp
        )
    }
}
