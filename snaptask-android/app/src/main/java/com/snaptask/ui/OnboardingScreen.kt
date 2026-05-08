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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptask.ui.theme.*

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowScale by pulse.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
    ) {
        // Top glow
        Box(
            modifier = Modifier
                .size(360.dp)
                .scale(glowScale)
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .background(
                    Brush.radialGradient(listOf(ColorAccent.copy(alpha = 0.18f), Color.Transparent)),
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
            Spacer(Modifier.height(32.dp))

            // Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(ColorAccent, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    text = "SnapTask",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(44.dp))

            // Hero text
            Text(
                text = "Your camera,\nnow with\nsuperpowers.",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Snap a photo. AI reads it.\nSamsung acts on it.",
                color = ColorMuted,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Feature cards
            FeatureCard(
                icon    = "📸",
                color   = Color(0xFF3B82F6),
                title   = "Snap anything",
                body    = "Business cards, event flyers, receipts, whiteboards — SnapTask reads them all."
            )
            Spacer(Modifier.height(10.dp))
            FeatureCard(
                icon    = "🤖",
                color   = ColorAccent,
                title   = "Gemini understands it",
                body    = "AI extracts every useful detail and figures out exactly what to do with it."
            )
            Spacer(Modifier.height(10.dp))
            FeatureCard(
                icon    = "⚡",
                color   = Color(0xFF22C55E),
                title   = "Samsung executes it",
                body    = "Calendar events, contacts, notes, expenses — created in one tap."
            )

            Spacer(Modifier.weight(1f))

            // CTA
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = ColorBg
                )
            ) {
                Text("Get Started  →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "🔒  Images never leave your device",
                color = ColorMuted.copy(alpha = 0.55f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FeatureCard(icon: String, color: Color, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color.copy(alpha = 0.13f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 24.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(body, color = ColorMuted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
