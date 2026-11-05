package com.mono.systools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.layerBackdrop
import com.mono.systools.core.Cfg
import com.mono.systools.ui.SettingsScreen
import com.mono.systools.ui.rememberGlassBackdrop

/**
 * 模块设置页。
 *
 * 层级：
 *   Box(base color)
 *     ├─ Box.layerBackdrop(backdrop)  ← 极光背景，作为玻璃的折射源
 *     └─ SettingsScreen               ← 所有玻璃组件 drawBackdrop 于其上
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(Cfg.PREFS_NAME, MODE_PRIVATE)

        setContent {
            val backdrop = rememberGlassBackdrop()

            Box(Modifier.fillMaxSize().background(Color(0xFF06070C))) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    AuroraBackground()
                }

                SettingsScreen(
                    backdrop = backdrop,
                    prefs = prefs,
                    onOpenTarget = { openTarget() }
                )
            }
        }
    }

    private fun openTarget() {
        runCatching {
            val intent = packageManager.getLaunchIntentForPackage(Cfg.TARGET_PKG)
                ?: return@runCatching
            startActivity(intent)
        }
    }
}

/**
 * 背景：深色渐变 + 三团高饱和光斑。
 * 玻璃卡片折射的就是这块内容 —— 光斑位置决定了玻璃的"通透感"。
 */
@Composable
private fun AuroraBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color(0xFF080A14), Color(0xFF131029), Color(0xFF1C0E24)),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )

        val blobs = listOf(
            Triple(0.12f, 0.10f, Color(0x70FF6FA5)),
            Triple(0.88f, 0.28f, Color(0x605A6BFF)),
            Triple(0.45f, 0.92f, Color(0x508A5CFF))
        )
        blobs.forEach { (fx, fy, color) ->
            val center = Offset(size.width * fx, size.height * fy)
            val radius = size.minDimension * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}
