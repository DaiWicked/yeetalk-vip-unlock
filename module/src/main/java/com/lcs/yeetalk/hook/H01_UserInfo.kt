package com.lcs.yeetalk.hook

import com.lcs.yeetalk.core.Cfg
import com.lcs.yeetalk.core.LogX
import com.lcs.yeetalk.core.Prefs
import java.lang.reflect.Field

/**
 * H01 · 最小可用闭环（MVP）
 *
 * 目标：com.imback.commonbase.entity.UserInfo
 *
 * 判定链（2.58.2 已核实）：
 *   K() = is_vip == 1                       ← 总闸
 *   H() = vip_expire > 0 && !K()            ← "曾经是 VIP 但已过期"
 *   I() = H() && 过期<10天    J() = H() && 过期>=10天
 *   L() = 距过期<10天         N() = 距过期>=10天
 *
 * 策略：K=true（有效会员），H/I/J/L/N=false（不走"已过期/即将到期"分支，
 *       否则会显示"会员已过期"并弹挽留窗，比原状态更糟）。
 */
object H01UserInfo {

    fun install(cl: ClassLoader) {
        if (!Prefs.unit(Cfg.K.H_USER_INFO)) {
            LogX.i("H01 已关闭，跳过")
            return
        }

        val clazz = HookUtil.clazz(Cfg.T.USER_INFO, cl) ?: return

        val fVip: Field? = HookUtil.resolveField(clazz, Cfg.A.IS_VIP, Cfg.F.IS_VIP)
        val fExp: Field? = HookUtil.resolveField(clazz, Cfg.A.VIP_EXPIRE, Cfg.F.VIP_EXPIRE)

        LogX.i("H01 字段定位：is_vip=${fVip?.name ?: "未找到"}, vip_expire=${fExp?.name ?: "未找到"}")

        // 1) 构造后注入：覆盖 Parcel / new 出来的实例
        var built = 0
        HookUtil.afterAllConstructors(clazz) { obj ->
            HookUtil.setInt(obj, fVip, 1)
            HookUtil.setInt(obj, fExp, HookUtil.vipExpireSec())
            if (built++ < 3) {
                val e = runCatching { fVip?.let { it.isAccessible = true; it.getInt(obj) } }.getOrNull()
                val x = runCatching { fExp?.let { it.isAccessible = true; it.getInt(obj) } }.getOrNull()
                LogX.i("H01 构造注入#$built：E=$e F=$x")
            }
        }

        // 2) 状态读取方法翻转（真正生效的一层，抗 Gson 反序列化绕过构造的情况）
        HookUtil.hookTraced(clazz, "K", true, 60)

        HookUtil.hookBool(clazz, "H", false)
        HookUtil.hookBool(clazz, "I", false)
        HookUtil.hookBool(clazz, "J", false)
        HookUtil.hookBool(clazz, "L", false)
        HookUtil.hookBool(clazz, "N", false)

        LogX.i("H01 已装载")
    }
}
