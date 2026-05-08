package com.snaptask.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.snaptask.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class Page(
    val tag: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val accentDark: Color,
)

private val pages = listOf(
    Page(
        tag      = "CAPTURE",
        emoji    = "📸",
        title    = "Snap anything\nwith text",
        subtitle = "Business cards, event flyers, receipts,\nwhiteboards — just point and shoot.",
        accent   = Color(0xFF3B82F6),
        accentDark = Color(0xFF1E40AF),
    ),
    Page(
        tag      = "UNDERSTAND",
        emoji    = "✨",
        title    = "AI reads &\nunderstands it",
        subtitle = "Llama AI extracts every detail and\nfigures out exactly what to do next.",
        accent   = ColorAccent,
        accentDark = Color(0xFF4338CA),
    ),
    Page(
        tag      = "EXECUTE",
        emoji    = "⚡",
        title    = "Done in\none tap",
        subtitle = "Calendar events, contacts, notes,\nexpenses — created automatically.",
        accent   = Color(0xFF10B981),
        accentDark = Color(0xFF065F46),
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
    ) {
        // Animated ambient glow behind hero
        val currentAccent = pages[pagerState.currentPage].accent
        val pulse = rememberInfiniteTransition(label = "glow")
        val glowScale by pulse.animateFloat(
            initialValue = 0.85f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(3500, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "scale"
        )
        Box(
            modifier = Modifier
                .size(420.dp)
                .scale(glowScale)
                .align(Alignment.Center)
                .offset(y = (-60).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(currentAccent.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(ColorAccent, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("S", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                    Text("SnapTask", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }

                // Skip
                if (!isLastPage) {
                    TextButton(onClick = onGetStarted) {
                        Text("Skip", color = ColorMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Spacer(Modifier.width(64.dp))
                }
            }

            // ── Pager ────────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { index ->
                PageContent(
                    page = pages[index],
                    pagerState = pagerState,
                    pageIndex = index,
                )
            }

            // ── Bottom section ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { i ->
                        PagerDot(
                            selected = i == pagerState.currentPage,
                            color = pages[pagerState.currentPage].accent
                        )
                    }
                }

                // CTA Button
                Button(
                    onClick = {
                        if (isLastPage) onGetStarted()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pages[pagerState.currentPage].accent,
                        contentColor   = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (isLastPage) "Get Started  →" else "Continue  →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }

                Text(
                    text = "🔒  Your images never leave your device",
                    color = ColorMuted.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageContent(page: Page, pagerState: PagerState, pageIndex: Int) {
    val pageOffset = (pagerState.currentPage - pageIndex + pagerState.currentPageOffsetFraction)
        .absoluteValue.coerceIn(0f, 1f)
    val scale = lerp(1.dp, 0.88.dp, pageOffset).value
    val alpha = lerp(1.dp, 0.4.dp, pageOffset).value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .scale(scale)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero icon
        Box(contentAlignment = Alignment.Center) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(196.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(page.accent.copy(alpha = 0.18f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            // Inner gradient circle
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(page.accent, page.accentDark)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(page.emoji, fontSize = 56.sp)
            }
        }

        Spacer(Modifier.height(36.dp))

        // Tag pill
        Box(
            modifier = Modifier
                .background(page.accent.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                page.tag,
                color = page.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.title,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = page.subtitle,
            color = ColorMuted,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PagerDot(selected: Boolean, color: Color) {
    val width by animateDpAsState(
        targetValue = if (selected) 24.dp else 7.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dot"
    )
    Box(
        modifier = Modifier
            .height(7.dp)
            .width(width)
            .clip(CircleShape)
            .background(if (selected) color else ColorBorder)
    )
}
