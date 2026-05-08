package com.snaptask.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG     = Color(0xFF0D0D14)
private val ACCENT = Color(0xFF6366F1)
private val MUTED  = Color(0xFF9CA3AF)
private val CARD   = Color(0xFF13131C)

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue  = 0.13f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
    ) {
        // Ambient glow top-center
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = (-80).dp)
                .background(ACCENT.copy(alpha = glowAlpha), CircleShape)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Wordmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(7.dp).background(ACCENT, CircleShape))
                Text(
                    text = "SnapTask",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(Modifier.height(52.dp))

            Text(
                text = "Your camera,\nnow with superpowers.",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Point at anything with text and watch it\nturn into a real action — instantly.",
                color = MUTED,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Feature cards
            FeatureRow(
                icon  = "📸",
                title = "Snap anything",
                body  = "Business cards, event flyers, receipts, whiteboards — if it has text, SnapTask reads it."
            )
            Spacer(Modifier.height(12.dp))
            FeatureRow(
                icon  = "🤖",
                title = "AI understands it",
                body  = "Gemini classifies the intent and pulls out every relevant detail automatically."
            )
            Spacer(Modifier.height(12.dp))
            FeatureRow(
                icon  = "✅",
                title = "Samsung acts on it",
                body  = "Calendar events, contacts, notes, and expenses — created in one tap."
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = BG
                )
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "🔒  Your images never leave your device.",
                color = MUTED.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CARD, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(ACCENT.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text  = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = body,
                color = MUTED,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
