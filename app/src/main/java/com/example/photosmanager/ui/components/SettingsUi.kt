package com.example.photosmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 全站共用的"设置项外观"约定，对应孪生项目 ScreenshotBye 的 themes.xml 里那几套文字样式：
 * SectionTitle / BodyHint / SettingRowLabel / SettingRowValue / SectionChevron。
 * 两边一个是 Views 一个是 Compose，抄不了 XML，但字号、颜色角色和行高必须一样——
 * 否则同样的两个应用并排打开，设置页看着就是两种东西。
 */

/**
 * 卡片：背景钉死成中性的 colorScheme.surface（每套配色里都是同一个 #EEEDEB / #2F2E30），
 * 不用 Material3 默认的容器色。默认那层在配色表里各带色温，换主题时卡片会跟着变色，
 * 而主题颜色本该只管按钮、旋钮这些控件。
 */
@Composable
fun PaletteCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val body: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
    if (onClick == null) {
        Card(modifier = modifier, colors = colors, content = body)
    } else {
        Card(modifier = modifier, onClick = onClick, colors = colors, content = body)
    }
}

/** 卡内小标题：15sp 加粗，onSurface，下方留 6dp（对齐 ScreenshotBye 的 SectionTitle） */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .padding(bottom = 6.dp),
        fontSize = 15.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** 说明文字：12sp，onSurfaceVariant，行距多 2dp（对齐 ScreenshotBye 的 BodyHint） */
@Composable
fun BodyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 行尾那个 ">"：它是文本不是图标，16sp、outline 色，和参考仓库一致 */
@Composable
fun SectionChevron(modifier: Modifier = Modifier) {
    Text(
        text = ">",
        modifier = modifier.padding(start = 6.dp),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.outline
    )
}

/**
 * 一行设置项：左标签（15sp）占满剩余宽度，右侧依次是可选的控件、当前值（13sp）和箭头。
 * 行高至少 48dp，横向内边距 16dp——和参考仓库里每个 SettingRow 的 minHeight 一样，
 * 不然同一张卡里开关行比跳转行矮一截，看着就乱了。
 *
 * [labelExtra] 紧跟在标题文字后面（参考仓库「无感删除」名字后面那个感叹号就是这么排的），
 * 不传就和以前一样；它和整行的点击是分开的两件事，别挤在同一次点击里。
 */
@Composable
fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    labelExtra: (@Composable () -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            labelExtra?.invoke()
        }
        trailing?.invoke()
        if (value != null) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SectionChevron()
        }
    }
}

/** 同一张卡里分组之间的细线，上下各留档距（默认 4dp，和 ScreenshotBye 的 MaterialDivider 一致） */
@Composable
fun SectionDivider(verticalMargin: Dp = 4.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = verticalMargin),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** 页面级空状态：居中一行 onSurfaceVariant 文字 */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 列表行左侧的缩略图尺寸，和参考仓库的 56dp 对齐 */
val ThumbnailSize = 56.dp
