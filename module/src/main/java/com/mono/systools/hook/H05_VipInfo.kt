package com.mono.systools.hook

import com.mono.systools.core.Cfg
import com.mono.systools.core.LogX
import com.mono.systools.core.Prefs

/**
 * H05 · 购买页 / 套餐页状态
 *
 * 目标：Bb.VipChargeInfo
 *   字段 isVip / vipExpireTime / tips / discountPropInfo
 *   方法 m() = isVip==1 && vipExpireTime >= now   ← 页面据此显示"已是会员"
 *
 * 注意：本单元只改**展示态**，不触碰下单/支付流程。
 */
object H05VipInfo {

    fun install(cl: ClassLoader) {
        if (!Prefs.unit(Cfg.K.H_VIPINFO)) {
            LogX.i("H05 已关闭，跳过")
            return
        }
        val clazz = HookUtil.clazz(Cfg.T.VIP_CHARGE_INFO, cl) ?: return

        val fVip = HookUtil.fieldByName(clazz, "isVip")
        val fExp = HookUtil.fieldByName(clazz, "vipExpireTime")

        LogX.i("H05 字段：isVip=${fVip?.name ?: "-"}, vipExpireTime=${fExp?.name ?: "-"}")

        HookUtil.afterAllConstructors(clazz) { obj ->
            HookUtil.setInt(obj, fVip, 1)
            HookUtil.setInt(obj, fExp, HookUtil.vipExpireSec())
        }

        // m() 是判定方法；不同版本可能重载，用 truthy 覆盖
        HookUtil.hookTruthy(clazz, "m")
        HookUtil.hookTruthy(clazz, "getIsVip")

        LogX.i("H05 已装载")
    }
}
