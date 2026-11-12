package com.hashcore.pool.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreSuccess
import com.hashcore.pool.presentation.theme.HashCoreTextMuted

/** Subtle pulse next to LIVE badge when socket is connected. */
@Composable
fun LivePulseDot(
    active: Boolean,
    color: Color = HashCoreSuccess,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = if (active) 1f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Box(
        modifier = modifier
            .size(10.dp)
            .scale(if (active) scale else 1f)
            .alpha(if (active) alpha else 0.45f)
            .clip(CircleShape)
            .background(color),
    )
}

/** Simple skeleton placeholder cards while dashboard loads. */
@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skel")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skelAlpha",
    )
    Column(modifier = modifier.padding(16.dp)) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (i == 0) 96.dp else 72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .alpha(alpha)
                    .background(HashCoreCard),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            "Loading dashboard…",
            style = MaterialTheme.typography.labelMedium,
            color = HashCoreTextMuted,
        )
    }
}

@Composable
fun SetupStepRow(
    step: Int,
    title: String,
    done: Boolean,
    detail: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (done) HashCoreAccent else HashCoreBorder),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (done) "✓" else step.toString(),
                color = if (done) Color.Black else Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (done) HashCoreAccent else Color.White,
            )
            Text(detail, style = MaterialTheme.typography.bodySmall, color = HashCoreTextMuted)
        }
    }
}
