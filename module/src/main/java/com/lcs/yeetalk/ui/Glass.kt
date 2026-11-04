package com.lcs.yeetalk.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur

/*
 * Liquid Glass 基础件。
 *
 * 底层：Kyant0/Backdrop 2.0.1（Apache-2.0）
 *   Modifier.drawBackdrop(backdrop, shape, effects, onDrawSurface)
 *   背景内容由 rememberLayerBackdrop() 捕获后折射/模糊。
 *
 * 设计取舍：
 *   1. 只用 blur()，不引入 lens()/vibrancy()，减少对库内部 API 的耦合
 *   2. 形状一律 RoundedCornerShape，不用 kyant-shapes 的 Capsule()
 *
 * 若提示 blur / drawBackdrop 未解析，把 import 改成：
 *   com.kyant.backdrop.effects.*  /  com.kyant.backdrop.drawBackdrop
 */

/** 创建一个与页面背景绑定的玻璃源；配合 Modifier.layerBackdrop 使用 */
@Composable
fun rememberGlassBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/**
 * 玻璃卡片：折射背景 + 半透明着色。
 */
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    tintAlpha: Float = 0.10f,
    tintColor: Color = Color.White,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = { blur(14f.dp.toPx()) },
                onDrawSurface = {
                    drawRect(tintColor.copy(alpha = tintAlpha))
                }
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * 玻璃开关：轨道自身也是玻璃，滑块为纯白实体。
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Color(0xFF34C759)
) {
    val trackW = 52.dp
    val trackH = 32.dp
    val thumb = 26.dp
    val travel = trackW - thumb - 6.dp

    val fraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "glassToggle"
    )

    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .size(width = trackW, height = trackH)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = { blur(8f.dp.toPx()) },
                onDrawSurface = {
                    drawRect(
                        if (checked) accent.copy(alpha = if (enabled) 0.88f else 0.35f)
                        else Color(0xFF808080).copy(alpha = 0.28f)
                    )
                }
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = travel * fraction)
                .size(thumb)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (enabled) 0.96f else 0.6f))
        )
    }
}

/**
 * 玻璃胶囊按钮
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tintAlpha: Float = 0.16f
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .height(46.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = { blur(10f.dp.toPx()) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = tintAlpha)) }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassText(text = text, emphasis = true)
    }
}

/** 分组标题 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    GlassText(
        text = text,
        modifier = modifier.padding(start = 8.dp, top = 18.dp, bottom = 8.dp),
        alpha = 0.55f,
        sizeSp = 12f
    )
}

/** 行：左标题+副标题，右组件 */
@Composable
fun GlassRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            GlassText(title, emphasis = true)
            if (!subtitle.isNullOrBlank()) {
                GlassText(subtitle, alpha = 0.55f, sizeSp = 12f)
            }
        }
        trailing()
    }
}

/** 统一文字：白字 + 可调透明度，避免依赖 MaterialTheme */
@Composable
fun GlassText(
    text: String,
    modifier: Modifier = Modifier,
    alpha: Float = 0.95f,
    sizeSp: Float = 15f,
    emphasis: Boolean = false
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = Color.White.copy(alpha = alpha),
        fontSize = sizeSp.sp,
        fontWeight = if (emphasis) androidx.compose.ui.text.font.FontWeight.SemiBold
        else androidx.compose.ui.text.font.FontWeight.Normal,
        lineHeight = (sizeSp * 1.45f).sp
    )
}
