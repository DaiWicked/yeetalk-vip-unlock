package com.mono.systools

import android.app.AndroidAppHelper
import com.mono.systools.core.Cfg
import com.mono.systools.core.LogX
import com.mono.systools.core.Prefs
import com.mono.systools.hook.H01UserInfo
import com.mono.systools.hook.H02Manager
import com.mono.systools.hook.H03Visitor
import com.mono.systools.hook.H04Greet
import com.mono.systools.hook.H05VipInfo
import com.mono.systools.hook.H06Dump
import com.mono.systools.hook.H07Generic
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Xposed 入口（由 assets/xposed_init 声明）。
 *
 * 纪律：
 *  - 只处理 com.imback.palpal，其余包名立即返回（不拖慢其它 App 启动）
 *  - 每个单元独立 try/catch，单点失败不影响其它单元
 *  - 版本不符不阻断，只告警 —— 所有 hook 都是"找不到就跳过"的防御式写法
 */
class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != Cfg.TARGET_PKG) return

        runCatching {
            val versionName = runCatching {
                AndroidAppHelper.currentApplication()
                    ?.packageManager
                    ?.getPackageInfo(lpparam.packageName, 0)
                    ?.versionName
            }.getOrNull()

            Prefs.initRemote()

            if (!Prefs.master()) {
                LogX.i("总开关关闭，本次不注入（${versionName ?: "?"}）")
                return
            }

            LogX.i("========== [YTVIP] 开始注入 ==========")
            LogX.i("目标：${lpparam.packageName}  version=$versionName  期望=${Cfg.SUPPORTED_VERSION}  process=${lpparam.processName}")

            if (versionName != null && versionName != Cfg.SUPPORTED_VERSION) {
                LogX.w("版本不匹配，混淆坐标可能已变；仍以防御模式装载")
            }

            val cl = lpparam.classLoader

            safe("H01") { H01UserInfo.install(cl) }
            safe("H02") { H02Manager.install(cl) }
            safe("H03") { H03Visitor.install(cl) }
            safe("H04") { H04Greet.install(cl) }
            safe("H05") { H05VipInfo.install(cl) }
            safe("H06") { H06Dump.install(cl) }
            safe("H07") { H07Generic.install(cl) }

            LogX.i("========== [YTVIP] 注入结束 ==========")
        }.onFailure { LogX.e("HookEntry 顶层异常", it) }
    }

    private inline fun safe(name: String, block: () -> Unit) {
        runCatching { block() }.onFailure { LogX.e("$name 装载失败", it) }
    }
}
