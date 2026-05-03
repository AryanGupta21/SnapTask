package com.snaptask

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.snaptask.ui.HistoryScreen
import com.snaptask.ui.theme.SnapTaskTheme
import java.io.File

private val BG      = Color(0xFF0D0D14)
private val PRIMARY = Color(0xFFFFFFFF)
private val MUTED   = Color(0xFF6B7280)
private val ACCENT  = Color(0xFF6366F1)

private enum class Screen { Home, History }

class MainActivity : ComponentActivity() {

    private var pendingCameraUri: Uri? = null
    private var refreshKey by mutableIntStateOf(0)

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
                var screen by remember { mutableStateOf(Screen.Home) }
                when (screen) {
                    Screen.Home -> HomeScreen(
                        context = this,
                        refreshKey = refreshKey,
                        onSnap = { launchCamera() },
                        onGallery = {
                            pickFromGallery.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onViewHistory = { screen = Screen.History }
                    )
                    Screen.History -> HistoryScreen(
                        context = this,
                        onBack = { screen = Screen.Home },
                        onClear = { refreshKey++ }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKey++
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) openCamera()
        else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val file = File(cacheDir, "camera/snap_${System.currentTimeMillis()}.jpg")
            .also { it.parentFile?.mkdirs() }
        pendingCameraUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        takePicture.launch(pendingCameraUri!!)
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
fun HomeScreen(
    context: Context,
    refreshKey: Int,
    onSnap: () -> Unit,
    onGallery: () -> Unit,
    onViewHistory: () -> Unit,
) {
    val totalActions = remember(refreshKey) { ActionHistory.load(context).size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // Wordmark with accent dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(ACCENT, CircleShape)
                )
                Text(
                    text = "SnapTask",
                    color = MUTED,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Point at anything\nwith text.",
                color = PRIMARY,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Poster, receipt, business card,\nwhiteboard — we'll handle the rest.",
                color = MUTED,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(60.dp))

            SnapButton(onClick = onSnap)

            Spacer(Modifier.height(28.dp))

            TextButton(onClick = onGallery) {
                Text(
                    text = "Use a photo instead",
                    color = MUTED,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = totalActions > 0,
                enter = fadeIn(tween(600))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { onViewHistory() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(ACCENT.copy(alpha = 0.6f), CircleShape)
                    )
                    Text(
                        text = "$totalActions action${if (totalActions == 1) "" else "s"} saved",
                        color = MUTED.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                    Text(text = "→", color = MUTED.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SnapButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else pulseScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow rings — scale with pulse so they breathe together
        listOf(236.dp to 0.07f, 260.dp to 0.04f, 288.dp to 0.02f).forEach { (size, alpha) ->
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(if (pressed) 0.92f else pulseScale)
                    .background(ACCENT.copy(alpha = alpha), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pressScale)
                .background(PRIMARY, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = { onClick() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Snap",
                    color = BG,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "it",
                    color = BG.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
