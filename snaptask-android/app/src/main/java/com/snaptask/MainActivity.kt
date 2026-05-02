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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.snaptask.ui.theme.SnapTaskTheme
import java.io.File

// Radar color palette
private val BG = Color(0xFF020907)
private val RADAR_GREEN = Color(0xFF00FF41)
private val DIM_GREEN = RADAR_GREEN.copy(alpha = 0.12f)
private val BLIP_CALENDAR = Color(0xFF60A5FA)
private val BLIP_CONTACT = Color(0xFF34D399)
private val BLIP_NOTE = Color(0xFFFBBF24)
private val BLIP_EXPENSE = Color(0xFFF87171)

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
                RadarScreen(
                    context = this,
                    refreshKey = refreshKey,
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
fun RadarScreen(
    context: Context,
    refreshKey: Int,
    onScanCamera: () -> Unit,
    onPickGallery: () -> Unit,
) {
    val blips = remember(refreshKey) { ActionHistory.load(context) }
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "sweep"
    )
    val statusPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "status"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HudHeader(statusPulse)

            Spacer(Modifier.weight(1f))

            // Radar
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadarCanvas(
                        sweepAngle = sweepAngle,
                        blips = blips,
                        onTap = onScanCamera
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "[ TAP TO SCAN ]",
                        color = RADAR_GREEN.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 3.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            ActionStatsRow(blips)

            // Gallery shortcut
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onPickGallery) {
                    Text(
                        text = "or pick from gallery",
                        color = RADAR_GREEN.copy(alpha = 0.35f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HudHeader(statusPulse: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "■  SNAPTASK",
                color = RADAR_GREEN,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "AI VISION  v1.0",
                color = RADAR_GREEN.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "●",
                    color = RADAR_GREEN.copy(alpha = statusPulse),
                    fontSize = 9.sp
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "READY",
                    color = RADAR_GREEN,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "SYS  OK",
                color = RADAR_GREEN.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun RadarCanvas(
    sweepAngle: Float,
    blips: List<ActionHistory.Blip>,
    onTap: () -> Unit,
) {
    Canvas(
        modifier = Modifier
            .size(280.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
    ) {
        val radarRadius = size.minDimension / 2f * 0.92f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radarCenter = Offset(cx, cy)

        // Concentric rings
        listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { f ->
            drawCircle(DIM_GREEN, radarRadius * f, radarCenter, style = Stroke(1f))
        }

        // Cross hairs
        val lineAlpha = 0.08f
        drawLine(RADAR_GREEN.copy(lineAlpha), Offset(cx - radarRadius, cy), Offset(cx + radarRadius, cy), 1f)
        drawLine(RADAR_GREEN.copy(lineAlpha), Offset(cx, cy - radarRadius), Offset(cx, cy + radarRadius), 1f)

        // Sweep trail — 10 arc segments with fading alpha
        rotate(sweepAngle, pivot = radarCenter) {
            val segDeg = 9f
            repeat(10) { i ->
                val alpha = ((10 - i).toFloat() / 10f) * 0.55f
                drawArc(
                    color = RADAR_GREEN.copy(alpha = alpha),
                    startAngle = -90f - (i + 1) * segDeg,
                    sweepAngle = segDeg,
                    useCenter = true,
                    topLeft = Offset(cx - radarRadius, cy - radarRadius),
                    size = Size(radarRadius * 2, radarRadius * 2)
                )
            }
            // Leading edge line
            drawLine(
                color = RADAR_GREEN,
                start = radarCenter,
                end = Offset(cx, cy - radarRadius),
                strokeWidth = 1.5f
            )
        }

        // Blips
        blips.forEach { blip ->
            val ageMin = (System.currentTimeMillis() - blip.timestamp) / 60_000f
            val alpha = (1f - ageMin / 60f).coerceIn(0.1f, 1f)
            val bx = cx + blip.x * radarRadius
            val by = cy + blip.y * radarRadius
            val offset = Offset(bx, by)
            val color = blipColor(blip.type)
            // Outer glow
            drawCircle(color.copy(alpha = alpha * 0.15f), 14f, offset)
            drawCircle(color.copy(alpha = alpha * 0.4f), 6f, offset)
            // Core dot
            drawCircle(color.copy(alpha = alpha), 3f, offset)
        }

        // Center dot
        drawCircle(RADAR_GREEN.copy(alpha = 0.6f), 3f, radarCenter)
    }
}

@Composable
private fun ActionStatsRow(blips: List<ActionHistory.Blip>) {
    val calendar = blips.count { it.type == "create_calendar_event" }
    val contacts = blips.count { it.type == "create_contact" }
    val notes = blips.count { it.type == "create_note" || it.type == "log_expense" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("EVENTS", calendar, BLIP_CALENDAR)
        StatDivider()
        StatItem("CONTACTS", contacts, BLIP_CONTACT)
        StatDivider()
        StatItem("NOTES", notes, BLIP_NOTE)
    }
}

@Composable
private fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%02d".format(count),
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(RADAR_GREEN.copy(alpha = 0.1f), RoundedCornerShape(1.dp))
    )
}

private fun blipColor(type: String): Color = when {
    type.contains("calendar") -> BLIP_CALENDAR
    type.contains("contact") -> BLIP_CONTACT
    type.contains("expense") -> BLIP_EXPENSE
    type.contains("note") -> BLIP_NOTE
    else -> RADAR_GREEN
}
