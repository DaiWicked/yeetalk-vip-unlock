package com.lcs.yeetalk.hook

import com.lcs.yeetalk.core.Cfg
import com.lcs.yeetalk.core.LogX
import com.lcs.yeetalk.core.Prefs

/**
 * H06 · 权益全表 dump（只读）
 *
 * 目标：Bb.VipPrivilegeTableItemInfo
 *   字段 code / name / vipTableValue / tableValue / tableType
 *
 * 目的：把服务端下发的**官方权益清单**打印出来，这样"能解锁哪些权益"
 *       不用靠猜 —— 页面渲染了什么，这里就有什么。
 *
 * 这一层不改任何值，纯观测，无副作用。
 */
object H06Dump {

    private val KEY_FIELDS = listOf("code", "name", "vipTableValue", "tableValue", "tableType")
    private val dumped = HashSet<String>()

    fun install(cl: ClassLoader) {
        if (!Prefs.unit(Cfg.K.H_DUMP)) {
            LogX.i("H06 已关闭，跳过")
            return
        }
        val clazz = HookUtil.clazz(Cfg.T.VIP_PRIVILEGE_ITEM, cl) ?: return

        // 挂构造：每个权益项被创建时打一行
        HookUtil.afterAllConstructors(clazz) { obj ->
            runCatching {
                val parts = KEY_FIELDS.mapNotNull { n ->
                    val f = HookUtil.fieldByName(clazz, n) ?: return@mapNotNull null
                    f.isAccessible = true
                    val v = runCatching { f.get(obj) }.getOrNull()
                    "$n=$v"
                }
                val line = parts.joinToString(" | ")
                if (line.isNotEmpty() && dumped.add(line)) {
                    LogX.i("H06 权益项：$line")
                }
            }
        }

        LogX.i("H06 已装载（权益表 dump 开启）")
    }
}
