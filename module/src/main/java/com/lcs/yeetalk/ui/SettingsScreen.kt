package com.lcs.yeetalk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lcs.yeetalk.core.Cfg
import com.kyant.backdrop.backdrops.LayerBackdrop

// 数据类不能叫 Unit：会遮蔽 kotlin.Unit，让所有 `() -> Unit` 回调被解析成
// `() -> com.lcs.yeetalk.ui.Unit`，编译期报类型不匹配。
private data class HookUnit(val key: String, val title: String, val desc: String)

private val UNITS = listOf(
    HookUnit(Cfg.K.H_USER_INFO, "VIP 状态", "✅ 实测生效 · is_vip / vip_expire 注入 + K() 翻转"),
    HookUnit(Cfg.K.H_MANAGER, "VIP 统一出口", "✅ 实测生效 · UserInfoManager.C() → 1，兜住不走 UserInfo 的页面"),
    HookUnit(Cfg.K.H_VISITOR, "访客解锁额度", "⚠️ 目标类被 R8 混淆，未定位（开关无效）"),
    HookUnit(Cfg.K.H_GREET, "打招呼次数", "⚠️ 目标类被 R8 混淆，未定位（开关无效）"),
    HookUnit(Cfg.K.H_VIPINFO, "套餐页已开通", "⚠️ 目标类被 R8 混淆，未定位（开关无效）"),
    HookUnit(Cfg.K.H_DUMP, "权益表 dump", "只读：把官方权益清单打到 logcat")
)

/**
 * 设置主界面。所有改动即时写入 SharedPreferences，
 * 目标 App 下次启动（或被强杀重开）时由 XSharedPreferences 读到。
 */
@Composable
fun SettingsScreen(
    backdrop: LayerBackdrop,
    prefs: android.content.SharedPreferences,
    onOpenTarget: () -> Unit
) {
    var master by remember { mutableStateOf(prefs.getBoolean(Cfg.K.MASTER, true)) }
    val units = remember {
        mutableStateMapOf<String, Boolean>().apply {
            UNITS.forEach { put(it.key, prefs.getBoolean(it.key, true)) }
        }
    }
    var unlockCount by remember { mutableIntStateOf(prefs.getInt(Cfg.K.UNLOCK_COUNT, 999)) }
    var manual by remember { mutableStateOf(prefs.getString(Cfg.K.H_GREET + "_manual", "") ?: "") }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ---------- 顶部标题 ----------
        GlassText("YeeTalk VIP", alpha = 0.98f, sizeSp = 30f, emphasis = true)
        GlassText(
            "本地权益解锁 · LSPosed 模块",
            alpha = 0.6f,
            sizeSp = 13f
        )
        Spacer(Modifier.height(18.dp))

        // ---------- 总开关 ----------
        GlassCard(backdrop, Modifier.fillMaxWidth(), tintAlpha = 0.16f) {
            GlassRow(
                title = "总开关",
                subtitle = if (master) "已启用，注入生效" else "已停用，目标 App 保持原样"
            ) {
                GlassToggle(master, {
                    master = it
                    prefs.edit().putBoolean(Cfg.K.MASTER, it).apply()
                }, backdrop)
            }
        }

        SectionTitle("HOOK 单元")

        // ---------- 各单元 ----------
        UNITS.forEachIndexed { i, u ->
            GlassCard(
                backdrop,
                Modifier.fillMaxWidth().padding(top = if (i == 0) 0.dp else 10.dp),
                tintAlpha = 0.10f
            ) {
                GlassRow(u.title, u.desc) {
                    GlassToggle(
                        checked = units[u.key] == true,
                        onCheckedChange = {
                            units[u.key] = it
                            prefs.edit().putBoolean(u.key, it).apply()
                        },
                        backdrop = backdrop,
                        enabled = master
                    )
                }
            }
        }

        SectionTitle("参数")

        // ---------- 访客额度 ----------
        GlassCard(backdrop, Modifier.fillMaxWidth(), tintAlpha = 0.10f) {
            GlassRow("访客解锁额度", "H03 目标类未定位，此项暂不生效；服务端另有限流") {
                GlassButton("−", {
                    unlockCount = (unlockCount - 100).coerceAtLeast(0)
                    prefs.edit().putInt(Cfg.K.UNLOCK_COUNT, unlockCount).apply()
                }, backdrop, Modifier.padding(end = 6.dp), tintAlpha = 0.12f)
                GlassText("$unlockCount", emphasis = true)
                GlassButton("+", {
                    unlockCount = (unlockCount + 100).coerceAtMost(99999)
                    prefs.edit().putInt(Cfg.K.UNLOCK_COUNT, unlockCount).apply()
                }, backdrop, Modifier.padding(start = 6.dp), tintAlpha = 0.12f)
            }
        }

        // ---------- 手动白名单 ----------
        GlassCard(backdrop, Modifier.fillMaxWidth().padding(top = 10.dp), tintAlpha = 0.10f) {
            GlassText("H04 手动白名单", emphasis = true)
            GlassText(
                "留空则只 dump 签名。填入方法名（逗号分隔）后，这些方法在目标进程里强制返回真值。",
                alpha = 0.55f,
                sizeSp = 12f
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = manual,
                onValueChange = {
                    manual = it
                    prefs.edit().putString(Cfg.K.H_GREET + "_manual", it).apply()
                },
                hint = "例如：a, isLimitReached, c",
                backdrop = backdrop
            )
        }

        SectionTitle("状态")

        GlassCard(backdrop, Modifier.fillMaxWidth(), tintAlpha = 0.10f) {
            GlassRow("目标应用", Cfg.TARGET_PKG + " · " + Cfg.SUPPORTED_VERSION)
            GlassRow("注入日志", "adb logcat -s XposedBridge | grep YTVIP")
            Spacer(Modifier.height(8.dp))
            GlassButton("打开 YeeTalk", onOpenTarget, backdrop, tintAlpha = 0.2f)
        }

        Spacer(Modifier.height(24.dp))
        GlassText(
            "改动即时保存；请强杀 YeeTalk 后重新打开。仅本地进程内存与展示层，不涉及服务端与支付。",
            alpha = 0.35f,
            sizeSp = 11f
        )
        Spacer(Modifier.height(40.dp))
    }
}
