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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

private data class Capability(val emoji: String, val label: String, val color: Color)

private val capabilities = listOf(
    Capability("📅", "Calendar",  Color(0xFF3B82F6)),
    Capability("👤", "Contacts",  Color(0xFF22C55E)),
    Capability("📝", "Notes",     Color(0xFFF97316)),
    Capability("💳", "Expenses",  Color(0xFFA855F7)),
)

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
        // Ambient glow behind snap button
        val pulse = rememberInfiniteTransition(label = "glow")
        val glowAlpha by pulse.animateFloat(
            initialValue = 0.10f, targetValue = 0.20f,
            animationSpec = infiniteRepeatable(tween(2800, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "alpha"
        )
        Box(
            modifier = Modifier
                .size(480.dp)
                .align(Alignment.Center)
                .offset(y = 60.dp)
                .background(
                    Brush.radialGradient(listOf(ColorAccent.copy(alpha = glowAlpha), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ──────────────────────────────────────────────
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
                            .size(32.dp)
                            .background(
                                Brush.linearGradient(listOf(ColorAccent, Color(0xFF4338CA))),
                                RoundedCornerShape(9.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("S", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text("SnapTask", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Samsung Prism", color = ColorMuted, fontSize = 10.sp, letterSpacing = 0.3.sp)
                    }
                }

                AnimatedVisibility(
                    visible = totalActions > 0,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it }
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ColorAccent.copy(alpha = 0.15f))
                            .border(1.dp, ColorAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🕐", fontSize = 12.sp)
                            Text(
                                "$totalActions saved",
                                color = ColorAccentLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(0.5f))

            // ── Headline ─────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Snap it.",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 48.sp,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "AI does the rest.",
                    color = ColorAccentLight,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 48.sp,
                    letterSpacing = (-1).sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Point at any text — card, flyer, receipt, or\nwhiteboard — and watch it become an action.",
                color = ColorMuted,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // ── Capability chips ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                capabilities.forEach { cap ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cap.color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .border(1.dp, cap.color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(cap.emoji, fontSize = 18.sp)
                            Text(cap.label, color = cap.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(0.8f))

            // ── Snap button ──────────────────────────────────────────
            SnapButton(onClick = onSnap)

            Spacer(Modifier.height(24.dp))

            // ── Secondary actions ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onGallery,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.75f))
                ) {
                    Text("📷  Gallery", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.75f))
                ) {
                    Text("🕐  History", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SnapButton(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1.0f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press"
    )

    Box(contentAlignment = Alignment.Center) {
        // Pulsing glow rings
        listOf(180.dp to 0.10f, 210.dp to 0.06f, 240.dp to 0.03f).forEach { (size, alpha) ->
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .background(ColorAccent.copy(alpha = alpha), CircleShape)
            )
        }

        // Outer shutter ring
        Box(
            modifier = Modifier
                .size(148.dp)
                .scale(pressScale)
                .border(2.dp, ColorAccent.copy(alpha = 0.5f), CircleShape)
        )

        // Inner shutter ring
        Box(
            modifier = Modifier
                .size(136.dp)
                .scale(pressScale)
                .border(1.dp, ColorAccentLight.copy(alpha = 0.25f), CircleShape)
        )

        // Main button
        Box(
            modifier = Modifier
                .size(122.dp)
                .scale(pressScale)
                .background(
                    Brush.radialGradient(listOf(Color.White, Color(0xFFDDE0FF))),
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("📸", fontSize = 32.sp)
                Text(
                    "SNAP",
                    color = ColorBg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
