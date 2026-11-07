package com.example.photosmanager.ui.settings

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ViewConfiguration
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photosmanager.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 弧的起点：150° 在屏幕坐标系里是左下方 */
private const val START_ANGLE = 150f

/** 弧的张角。留 120° 缺口朝下，读数正好放在圆心 */
private const val SWEEP_ANGLE = 240f

/** 轨道半径占旋钮方框的比例 */
private const val TRACK_RADIUS_RATIO = 0.363f

/**
 * 可拨动的环带半宽（占方框比例）。只有落点距圆心在 [轨道半径-带宽, 轨道半径+带宽+档位数字外扩]
 * 这一段环带里才算"点在圆形轨道附近"，由旋钮接手；环带以内（圆心读数区）和以外（方框四角空白）
 * 都交还给页面滚动。这样卡片里除了轨道那圈，其余空白都能上下滑动。
 */
private const val TRACK_BAND = 0.17f

/**
 * 弧两端各往外多留一点余量。弧只张 240°，下方 120° 是缺口，那一段连轨道都没有，
 * 按下时必须让给页面滚动，不能被旋钮吃掉。
 */
private const val ARC_SLACK = 12f

/** 档位数字挂在弧外侧的外扩量 */
private const val LABEL_OUTLIFT = 0.09f

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
 *
 * 触摸范围刻意收成轨道那一圈环带：设置页整页可滚动，旋钮要是把整个方框都吃掉，
 * 用户在卡片里就滑不动页面了（孪生项目 ScreenshotBye 的 DetentDial 同样如此）。
 */
@Composable
fun DetentDial(
    label: String,
    value: Int,
    presets: List<Int>,
    @StringRes unitRes: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxSize: Dp = 190.dp
) {
    if (presets.size < 2) error(stringResource(R.string.dial_error_two_presets))

    val density = LocalDensity.current
    val context = LocalContext.current
    val unit = stringResource(unitRes)
    var index by remember(presets) { mutableIntStateOf(nearestIndex(value, presets)) }
    var lastTickAt by remember { mutableLongStateOf(0L) }
    val touchSlop = remember { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }

    // 外部改了 value（比如重新进设置页）时跟着归位
    LaunchedEffect(value, presets) {
        index = nearestIndex(value, presets)
    }

    fun select(target: Int) {
        if (target !in presets.indices) return
        if (target == index) return
        index = target
        // 一次快速划过好几档时只响一下，不然变成持续的嗡嗡声
        val now = SystemClock.uptimeMillis()
        if (now - lastTickAt > 25) {
            lastTickAt = now
            detentTick(context)
        }
        onValueChange(presets[target])
    }

    val primary = MaterialTheme.colorScheme.primary
    val trackColor = primary.copy(alpha = 0.15f)
    val dotColor = primary.copy(alpha = 0.35f)
    val onSurface = MaterialTheme.colorScheme.onSurface

    // 两个旋钮并排放的时候分不到 190dp，所以按父级给到的宽高自适应；
    // 本体再缩到 92%，给挂在弧外侧的档位数字留出去处，标签单独占上面一行。
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val labelReserve = with(density) { 14.sp.toDp() + 6.dp }
        val square = minOf(maxSize, maxWidth, maxHeight - labelReserve)
        val dialSize = square * 0.92f
        val radiusPx = with(density) { (dialSize * TRACK_RADIUS_RATIO).toPx() }
        val strokePx = with(density) { (dialSize * 0.047f).toPx() }
        val dotPx = with(density) { (dialSize * 0.013f).toPx() }
        val thumbPx = with(density) { (dialSize * 0.058f).toPx() }
        val labelGap = with(density) { (dialSize * LABEL_OUTLIFT).toPx() }
        val labelBox = dialSize * 0.19f
        val labelBoxPx = with(density) { labelBox.toPx() }
        val centerFontSize = (dialSize.value * 0.147f).sp
        val detentFontSize = (dialSize.value * 0.068f).coerceIn(10.5f, 13.5f).sp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // semantics 里不能直接调 stringResource（不是 composable 作用域），先在组合里取好
            val dialDescription =
                stringResource(R.string.dial_desc_with_label, label, presets[index], unit)

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(dialSize)
                    // 不做 mergeDescendants：一旦合并，周围那些可点击的档位节点会被吞进父节点，
                    // 读屏就只能报一个数字、没法逐档选择。
                    .semantics {
                        contentDescription = dialDescription
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = index.toFloat(),
                            range = 0f..(presets.size - 1).toFloat(),
                            steps = presets.size - 1
                        )
                    }
                    .pointerInput(presets, unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val sizePx = with(density) { dialSize.toPx() }
                            val center = Offset(sizePx / 2f, sizePx / 2f)
                            // 没按在轨道那圈上就不消费这次按下，让页面正常滚动
                            if (!nearTrack(down.position, sizePx)) return@awaitEachGesture
                            down.consume()
                            var dragging = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.isConsumed) break
                                // pressed=false 就是这一根手指离开（抬手或手势被系统抢走）
                                if (!change.pressed) {
                                    select(indexAt(change.position, center, presets.size))
                                    change.consume()
                                    return@awaitEachGesture
                                }
                                if (event.type == PointerEventType.Move) {
                                    if (!dragging) {
                                        // 没超过系统滑动阈值就先不动作，页面还留着滚动的余地
                                        if (change.position.distanceTo(down.position) < touchSlop) {
                                            change.consume()
                                            continue
                                        }
                                        dragging = true
                                    }
                                    select(indexAt(change.position, center, presets.size))
                                }
                                change.consume()
                            }
                        }
                    }
            ) {
                val sizePx = with(density) { dialSize.toPx() }
                val center = Offset(sizePx / 2f, sizePx / 2f)

                Canvas(modifier = Modifier.size(dialSize)) {
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
                    val anchor = polar(center, radiusPx + labelGap, angleFor(i, presets.size))
                    val detentDescription = stringResource(R.string.dial_value_desc, preset, unit)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                IntOffset(
                                    (anchor.x - labelBoxPx / 2f).roundToInt(),
                                    (anchor.y - labelBoxPx / 2f).roundToInt()
                                )
                            }
                            .size(labelBox)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { select(i) }
                            .semantics {
                                role = Role.RadioButton
                                selected = i == index
                                contentDescription = detentDescription
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.toString(),
                            fontWeight = if (i == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = detentFontSize,
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
                        fontSize = centerFontSize,
                        fontWeight = FontWeight.Normal,
                        color = primary
                    )
                    Text(
                        text = unit,
                        fontSize = (dialSize.value * 0.068f).sp,
                        color = onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/** 平面距离。Compose 各版本里 Offset 的取距离写法不一致，这里自己算，省得踩坑 */
private fun Offset.distanceTo(other: Offset): Float =
    hypot(x - other.x, y - other.y)

private fun nearestIndex(value: Int, presets: List<Int>): Int =
    presets.indices.minByOrNull { abs(presets[it] - value) } ?: 0

/**
 * 切档的那一下"咔"。
 *
 * 没用 Compose 的 LocalHapticFeedback：它把 LongPress 映射成系统的按键级轻触，
 * 不少 ROM 上那一档强度几乎摸不出来。这里直接开一次短震动，时长和强度自己定。
 * 也刻意不去读系统的"触摸反馈"开关：用户关掉它通常是不想让全局动作都跟着震，
 * 不等于不许应用给自己的一处交互加反馈。
 */
private fun detentTick(context: Context) {
    val vibrator = resolveVibrator(context) ?: return
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val amplitude = if (vibrator.hasAmplitudeControl()) 180 else VibrationEffect.DEFAULT_AMPLITUDE
        vibrator.vibrate(VibrationEffect.createOneShot(28, amplitude))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(28)
    }
}

private fun resolveVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

private fun angleFor(index: Int, count: Int): Float =
    START_ANGLE + SWEEP_ANGLE * index / (count - 1)

private fun polar(center: Offset, radius: Float, angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(
        center.x + radius * cos(rad).toFloat(),
        center.y + radius * sin(rad).toFloat()
    )
}

/**
 * 落点是否落在真的画了轨道的那段弧上：既要在环带半径内，也要在 240° 的张角内（两端各留一点余量）。
 * 圆心读数区和方框四角都不算，好让页面在那两处能正常滚动。
 */
private fun nearTrack(position: Offset, dialSizePx: Float): Boolean {
    val center = Offset(dialSizePx / 2f, dialSizePx / 2f)
    val r = dialSizePx * TRACK_RADIUS_RATIO
    val dist = position.distanceTo(center)
    val inner = (r - dialSizePx * TRACK_BAND).coerceAtLeast(0f)
    val outer = r + dialSizePx * (TRACK_BAND + LABEL_OUTLIFT)
    if (dist !in inner..outer) return false

    var deg = Math.toDegrees(
        atan2((position.y - center.y).toDouble(), (position.x - center.x).toDouble())
    ).toFloat()
    if (deg < 0f) deg += 360f
    val rel = (deg - START_ANGLE + 360f) % 360f
    return rel <= SWEEP_ANGLE + ARC_SLACK || rel >= 360f - ARC_SLACK
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
