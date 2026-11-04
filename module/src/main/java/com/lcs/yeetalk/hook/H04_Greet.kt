package com.lcs.yeetalk.hook

import com.lcs.yeetalk.core.Cfg
import com.lcs.yeetalk.core.LogX
import com.lcs.yeetalk.core.Prefs

/**
 * H04 · 打招呼次数限制
 *
 * 目标：Bb.GreetLimitChargeVipGuide
 *
 * 该类的内部方法全是混淆名（a/b/c…），无法在不逐行核对的情况下安全 hook。
 * 因此本单元做两件事：
 *   1. **dump** 全部方法签名到日志，供下一轮精准定位（不猜、不乱设）
 *   2. 允许用户在模块设置里手填方法名白名单（逗号分隔），命中才翻转真值
 *
 * 白名单来源：`adb shell cat /data/local/tmp/ytvip_manual.txt` 或设置页输入框。
 */
object H04Greet {

    fun install(cl: ClassLoader) {
        val clazz = HookUtil.clazz(Cfg.T.GREET_GUIDE, cl) ?: return

        // 1) dump：只在开启 H04 时执行
        if (Prefs.unit(Cfg.K.H_GREET)) {
            dump(clazz)
        }

        // 2) 手动白名单
        val manual = Prefs.str(Cfg.K.H_GREET + "_manual", "")
            .split(',', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (manual.isEmpty()) {
            LogX.i("H04 无手动白名单，仅 dump（把上方签名贴进设置页即可精准 hook）")
            return
        }

        manual.forEach { name ->
            HookUtil.hookTruthy(clazz, name)
        }
        LogX.i("H04 已装载，白名单=$manual")
    }

    private fun dump(clazz: Class<*>) {
        runCatching {
            LogX.i("H04 dump ${clazz.name}（共 ${clazz.declaredMethods.size} 个方法）")
            clazz.declaredMethods.sortedBy { it.name }.forEach { m ->
                val params = m.parameterTypes.joinToString(",") { it.simpleName }
                LogX.i("H04   ${m.returnType.simpleName} ${m.name}($params)")
            }
            clazz.declaredFields.forEach { f ->
                LogX.i("H04   field ${f.type.simpleName} ${f.name}")
            }
        }.onFailure { LogX.w("H04 dump 失败：${it.message}") }
    }
}
