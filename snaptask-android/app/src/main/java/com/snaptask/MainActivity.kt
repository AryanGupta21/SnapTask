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
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.snaptask.ui.HistoryScreen
import com.snaptask.ui.OnboardingScreen
import com.snaptask.ui.theme.*
import com.snaptask.ui.theme.SnapTaskTheme
import java.io.File

private enum class Screen { Onboarding, Home, History }

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
                var screen by remember { mutableStateOf(Screen.Onboarding) }
                when (screen) {
                    Screen.Onboarding -> OnboardingScreen(onGetStarted = {
                        screen = Screen.Home
                    })
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

    override fun onResume() { super.onResume(); refreshKey++ }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) openCamera()
        else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val file = File(cacheDir, "camera/snap_${System.currentTimeMillis()}.jpg")
            .also { it.parentFile?.mkdirs() }
        pendingCameraUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        takePicture.launch(pendingCameraUri!!)
    }

    private fun launchPipeline(uri: Uri) {
        startActivity(Intent(this, ShareReceiverActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
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
            .background(ColorBg)
    ) {
        // Radial gradient backdrop behind button
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.Center)
                .offset(y = 40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ColorAccent.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ColorAccent, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("S", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = "SnapTask",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(ColorSurfaceHigh, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Samsung Prism",
                        color = ColorMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.weight(0.6f))

            Text(
                text = "Point at anything\nwith text.",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 44.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Business cards, receipts, flyers, whiteboards\n— snap once, done.",
                color = ColorMuted,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(52.dp))

            SnapButton(onClick = onSnap)

            Spacer(Modifier.height(28.dp))

            OutlinedButton(
                onClick = onGallery,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
            ) {
                Text("📷   Choose from gallery", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = totalActions > 0,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it }
            ) {
                FilledTonalButton(
                    onClick = onViewHistory,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ColorAccent.copy(alpha = 0.15f),
                        contentColor   = ColorAccentLight
                    )
                ) {
                    Text(
                        text = "$totalActions action${if (totalActions == 1) "" else "s"} saved  →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SnapButton(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1.0f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else pulseScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow rings
        listOf(230.dp to 0.09f, 256.dp to 0.05f, 284.dp to 0.025f).forEach { (size, alpha) ->
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(if (pressed) 0.91f else pulseScale)
                    .background(ColorAccent.copy(alpha = alpha), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pressScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFE8E8FF))
                    ),
                    CircleShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap = { onClick() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Snap", color = ColorBg, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("it", color = ColorBg.copy(alpha = 0.35f), fontSize = 14.sp)
            }
        }
    }
}
