package com.example.photosmanager.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 弧的起点：150° 在屏幕坐标系里是左下方 */
private const val START_ANGLE = 150f

/** 弧的张角。留 120° 缺口朝下，读数正好放在圆心 */
private const val SWEEP_ANGLE = 240f

/**
 * 每次询问抽多少张。
 * 原先是 1-20 的连续滑块：20 个格子挤在一条轨道上，每格约 17dp，指尖一次压住两格，
 * 而且"8 张"和"12 张"体感上没什么区别，等于让用户猜。这里只留有区分度的档位。
 */
val SESSION_PRESETS = listOf(3, 5, 8, 10, 15, 20)

/** 一次拉进队列的"附近"照片数量，原范围 5-30 */
val NEARBY_PRESETS = listOf(5, 10, 15, 20, 30)

/**
 * 固定档位吸附旋钮。
 *
 * 只有 presets 里那几个值可选，手指在弧上怎么滑都会"咔"到最近的一格，
 * 所以不存在滑块那种在 20 个格子里瞄准的问题。
 * 每个档位数字自己也是可点击节点：手指能直接点中，读屏也能逐个走到。
 */
@Composable
fun DetentDial(
    label: String,
    value: Int,
    presets: List<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String = "张"
) {
    require(presets.size >= 2) { "DetentDial 至少需要两个档位" }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var index by remember(presets) { mutableIntStateOf(nearestIndex(value, presets)) }

    // 外部改了 value（比如重新进设置页）时跟着归位
    LaunchedEffect(value, presets) {
        index = nearestIndex(value, presets)
    }

    fun select(target: Int) {
        if (target !in presets.indices) return
        if (target == index) return
        index = target
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onValueChange(presets[target])
    }

    val dialSize = 190.dp
    val radiusPx = with(density) { (dialSize / 2 - 26.dp).toPx() }
    val strokePx = with(density) { 9.dp.toPx() }
    val dotPx = with(density) { 2.5.dp.toPx() }
    val thumbPx = with(density) { 11.dp.toPx() }
    val labelSize = with(density) { 40.dp.toPx() }
    val labelGap = with(density) { 19.dp.toPx() }

    val primary = MaterialTheme.colorScheme.primary
    val trackColor = primary.copy(alpha = 0.15f)
    val dotColor = primary.copy(alpha = 0.35f)
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = onSurface
        )

        // 最高那一档的数字标签会画到 Box 上沿之外，留出发位
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(dialSize)
                // 不做 mergeDescendants：一旦合并，周围那些可点击的档位节点会被吞进父节点，
                // 读屏就只能报一个数字、没法逐档选择。
                .semantics {
                    contentDescription = "$label，当前 ${presets[index]} $unit"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = index.toFloat(),
                        range = 0f..(presets.size - 1).toFloat(),
                        steps = presets.size - 1
                    )
                }
                .pointerInput(presets) {
                    detectTapGestures { position ->
                        select(indexAt(position, centerOf(size.width, size.height), presets.size))
                    }
                }
                .pointerInput(presets) {
                    detectDragGestures(
                        onDragStart = { position ->
                            select(indexAt(position, centerOf(size.width, size.height), presets.size))
                        },
                        onDragEnd = { },
                        onDragCancel = { }
                    ) { change, _ ->
                        select(indexAt(change.position, centerOf(size.width, size.height), presets.size))
                    }
                }
        ) {
            val sizePx = with(density) { dialSize.toPx() }

            Canvas(modifier = Modifier.matchParentSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val arcSize = Size(radiusPx * 2f, radiusPx * 2f)
                val arcTopLeft = Offset(c.x - radiusPx, c.y - radiusPx)
                val currentAngle = angleFor(index, presets.size)

                drawArc(
                    color = trackColor,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                val progressSweep = currentAngle - START_ANGLE
                if (progressSweep > 0f) {
                    drawArc(
                        color = primary,
                        startAngle = START_ANGLE,
                        sweepAngle = progressSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
                presets.indices.forEach { i ->
                    // 当前档不画小刻度，由指针大圆点代替
                    if (i != index) {
                        drawCircle(
                            color = dotColor,
                            radius = dotPx,
                            center = polar(c, radiusPx, angleFor(i, presets.size))
                        )
                    }
                }
                drawCircle(
                    color = primary,
                    radius = thumbPx,
                    center = polar(c, radiusPx, currentAngle)
                )
            }

            // 档位数字：既是刻度标签，也是可点击的选择项
            presets.forEachIndexed { i, preset ->
                val anchor = polar(
                    Offset(sizePx / 2f, sizePx / 2f),
                    radiusPx + labelGap,
                    angleFor(i, presets.size)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                (anchor.x - labelSize / 2f).roundToInt(),
                                (anchor.y - labelSize / 2f).roundToInt()
                            )
                        }
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { select(i) }
                        .semantics {
                            role = Role.RadioButton
                            selected = i == index
                            contentDescription = "$preset $unit"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (i == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (i == index) primary else onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = presets[index].toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = primary
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun nearestIndex(value: Int, presets: List<Int>): Int =
    presets.indices.minByOrNull { abs(presets[it] - value) } ?: 0

private fun angleFor(index: Int, count: Int): Float =
    START_ANGLE + SWEEP_ANGLE * index / (count - 1)

private fun centerOf(width: Int, height: Int): Offset =
    Offset(width / 2f, height / 2f)

private fun polar(center: Offset, radius: Float, angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(
        center.x + radius * cos(rad).toFloat(),
        center.y + radius * sin(rad).toFloat()
    )
}

/**
 * 手指位置 -> 档位下标。
 * 圆心里外都按角度算，落在下方缺口里则归到离它较近的那一端。
 */
private fun indexAt(position: Offset, center: Offset, count: Int): Int {
    val dx = position.x - center.x
    val dy = position.y - center.y
    if (dx == 0f && dy == 0f) return -1
    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (deg < 0f) deg += 360f
    val rel = (deg - START_ANGLE + 360f) % 360f
    if (rel > SWEEP_ANGLE) {
        return if (rel - SWEEP_ANGLE < (360f - SWEEP_ANGLE) / 2f) count - 1 else 0
    }
    return (rel / SWEEP_ANGLE * (count - 1)).roundToInt().coerceIn(0, count - 1)
}
