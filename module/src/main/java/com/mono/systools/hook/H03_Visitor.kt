package com.mono.systools.hook

import com.mono.systools.core.Cfg
import com.mono.systools.core.LogX
import com.mono.systools.core.Prefs

/**
 * H03 · 访客解锁额度
 *
 * 目标：ih.UserVisitorPageState（字段名未混淆）
 *   isVip / freeUnlockSize / maxAvailableFreeUnlockIndex / totalVisitorSize
 *
 * 做法：构造后把额度字段顶到配置值（默认 999），isVip 置 true。
 * 注意：这是**本地展示额度**；真正拉取访客数据仍可能被服务端限流，
 *       属于 PROJECT_PLAN 里的 B 层。
 */
object H03Visitor {

    private val AMOUNT_FIELDS = listOf("freeUnlockSize", "maxAvailableFreeUnlockIndex")

    fun install(cl: ClassLoader) {
        if (!Prefs.unit(Cfg.K.H_VISITOR)) {
            LogX.i("H03 已关闭，跳过")
            return
        }
        val clazz = HookUtil.clazz(Cfg.T.VISITOR_PAGE_STATE, cl) ?: return

        val amount = Prefs.int(Cfg.K.UNLOCK_COUNT, 999)
        val fVip = HookUtil.fieldByName(clazz, "isVip")
        val fAmounts = AMOUNT_FIELDS.mapNotNull { HookUtil.fieldByName(clazz, it) }

        LogX.i("H03 字段：isVip=${fVip?.name ?: "-"}, amounts=${fAmounts.joinToString { it.name }}")

        HookUtil.afterAllConstructors(clazz) { obj ->
            HookUtil.setInt(obj, fVip, 1)
            fAmounts.forEach { HookUtil.setInt(obj, it, amount) }
        }

        // 部分版本用 getter 暴露
        HookUtil.hookTruthy(clazz, "isVip")
        HookUtil.hookInt(clazz, "getFreeUnlockSize", amount)
        HookUtil.hookInt(clazz, "getMaxAvailableFreeUnlockIndex", amount)

        LogX.i("H03 已装载（额度=$amount）")
    }
}
