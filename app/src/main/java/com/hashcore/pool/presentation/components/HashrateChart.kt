package com.hashcore.pool.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hashcore.pool.domain.model.HashratePoint
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary
import com.hashcore.pool.util.HashrateFormatter
import com.hashcore.pool.util.TimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Hashrate chart with X/Y axis labels and long-press value popup.
 */
@Composable
fun HashrateChart(
    points: List<HashratePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = HashCoreAccent,
) {
    if (points.size < 2) {
        EmptyState(
            title = "No chart data",
            body = "Hashrate history will appear once shares are recorded.",
            modifier = modifier.height(200.dp),
        )
        return
    }

    val values = points.map { it.hashrateHs }
    val minV = values.minOrNull() ?: 0.0
    val maxV = values.maxOrNull() ?: 1.0
    val range = (maxV - minV).takeIf { it > 0 } ?: 1.0

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val padLeft = with(density) { 48.dp.toPx() }
    val padRight = with(density) { 12.dp.toPx() }
    val padTop = with(density) { 16.dp.toPx() }
    val padBottom = with(density) { 28.dp.toPx() }

    val yTicks = remember(minV, maxV) {
        listOf(maxV, minV + range * 0.5, minV)
    }
    val xLabelIndices = remember(points.size) {
        val last = points.lastIndex
        listOf(0, last / 2, last).distinct()
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .pointerInput(points) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                val plotW = size.width - padLeft - padRight
                                if (plotW <= 0f) return@detectTapGestures
                                val rel = ((offset.x - padLeft) / plotW).coerceIn(0f, 1f)
                                val idx = (rel * points.lastIndex).roundToInt()
                                    .coerceIn(0, points.lastIndex)
                                selectedIndex = idx
                                touchX = offset.x
                            },
                            onTap = { selectedIndex = null },
                        )
                    },
            ) {
                val plotW = size.width - padLeft - padRight
                val plotH = size.height - padTop - padBottom
                if (plotW <= 0f || plotH <= 0f) return@Canvas

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#64748B")
                    textSize = 28f
                    isAntiAlias = true
                }

                yTicks.forEach { tick ->
                    val yNorm = ((tick - minV) / range).toFloat()
                    val y = padTop + plotH * (1f - yNorm)
                    drawLine(
                        color = HashCoreBorder.copy(alpha = 0.45f),
                        start = Offset(padLeft, y),
                        end = Offset(padLeft + plotW, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        HashrateFormatter.format(tick),
                        4f,
                        y + 10f,
                        textPaint,
                    )
                }

                drawLine(
                    color = HashCoreBorder,
                    start = Offset(padLeft, padTop + plotH),
                    end = Offset(padLeft + plotW, padTop + plotH),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawLine(
                    color = HashCoreBorder,
                    start = Offset(padLeft, padTop),
                    end = Offset(padLeft, padTop + plotH),
                    strokeWidth = 1.5.dp.toPx(),
                )

                xLabelIndices.forEach { i ->
                    val x = padLeft + plotW * (i.toFloat() / points.lastIndex)
                    val label = points[i].label?.takeIf { it.isNotBlank() }
                        ?: timeFmt.format(Date(points[i].timestampEpochMs))
                    val tw = textPaint.measureText(label)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x - tw / 2,
                        size.height - 8f,
                        textPaint,
                    )
                }

                val stepX = plotW / points.lastIndex.coerceAtLeast(1)
                val path = Path()
                val fillPath = Path()
                points.forEachIndexed { i, p ->
                    val x = padLeft + i * stepX
                    val yNorm = ((p.hashrateHs - minV) / range).toFloat()
                    val y = padTop + plotH * (1f - yNorm)
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, padTop + plotH)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(padLeft + plotW, padTop + plotH)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0.02f)),
                        startY = padTop,
                        endY = padTop + plotH,
                    ),
                )
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )

                selectedIndex?.let { idx ->
                    val p = points[idx]
                    val x = padLeft + idx * stepX
                    val yNorm = ((p.hashrateHs - minV) / range).toFloat()
                    val y = padTop + plotH * (1f - yNorm)
                    drawLine(
                        color = lineColor.copy(alpha = 0.55f),
                        start = Offset(x, padTop),
                        end = Offset(x, padTop + plotH),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    )
                    drawCircle(color = lineColor, radius = 7.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }

            selectedIndex?.let { idx ->
                val p = points[idx]
                val popupWidthPx = with(density) { 160.dp.toPx() }
                val maxX = with(density) { 320.dp.toPx() }
                val xOff = (touchX - popupWidthPx / 2).coerceIn(8f, maxX)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(xOff.roundToInt(), 8) }
                        .padding(4.dp)
                        .background(HashCoreCard, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = HashrateFormatter.format(p.hashrateHs),
                        style = MaterialTheme.typography.titleMedium,
                        color = HashCoreAccent,
                    )
                    Text(
                        text = p.label?.takeIf { it.isNotBlank() } ?: TimeFormatter.dateTime(p.timestampEpochMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = HashCoreTextSecondary,
                    )
                    Text(
                        text = "Long-press for values · tap to dismiss",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = HashCoreTextMuted,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 8.dp),
        ) {
            Text(
                text = "Hashrate",
                style = MaterialTheme.typography.labelMedium,
                color = HashCoreTextMuted,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Time →",
                style = MaterialTheme.typography.labelMedium,
                color = HashCoreTextMuted,
            )
        }
    }
}
