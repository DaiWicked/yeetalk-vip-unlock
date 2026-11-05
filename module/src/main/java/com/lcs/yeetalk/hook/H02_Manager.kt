package com.lcs.yeetalk.hook

import com.lcs.yeetalk.core.Cfg
import com.lcs.yeetalk.core.LogX
import com.lcs.yeetalk.core.Prefs

/**
 * H02 · VIP 统一出口
 *
 * 目标：com.imback.commonbase.utils.UserInfoManager（Kotlin object）
 *
 *   C() = w().K() ? 1 : 0        ← 全 App 判断 VIP 的收敛点（返回 int）
 *   E() = C() == 1
 *   D() / F() / G()              ← 门控变体（返回类型未逐一确认，用 hookTruthy 自适应）
 *
 * 这一层是"下游兜底"：即使某个页面不读 UserInfo.K()，只要它走 UserInfoManager，
 * 也能拿到 VIP 状态。
 */
object H02Manager {

    fun install(cl: ClassLoader) {
        if (!Prefs.unit(Cfg.K.H_MANAGER)) {
            LogX.i("H02 已关闭，跳过")
            return
        }

        val clazz = HookUtil.clazz(Cfg.T.USER_INFO_MANAGER, cl) ?: return

        // C() 明确返回 int（带调用栈追踪，用于确认 UI 是否真的走这里）
        HookUtil.hookTraced(clazz, "C", 1, 60)

        // E/D/F/G 返回类型不确定，真值自适应（boolean → true，int → 1）
        listOf("E", "D", "F", "G").forEach { HookUtil.hookTruthy(clazz, it) }

        LogX.i("H02 已装载")
    }
}
