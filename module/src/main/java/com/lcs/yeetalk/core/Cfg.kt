package com.lcs.yeetalk.core

/**
 * 全局常量。目标 App 的类名/字段名一旦随版本变化，改这里即可，不用动 hook 逻辑。
 *
 * 数据来源：YeeTalk 2.58.2 (versionCode 1955) 反编译核实。
 * 详细坐标见 docs/PROJECT_PLAN.md。
 */
object Cfg {

    const val TAG = "YTVIP"

    /** 模块自己的包名（用于 XSharedPreferences 跨进程读配置） */
    const val MODULE_PKG = "com.lcs.yeetalk"

    /** 目标 App */
    const val TARGET_PKG = "com.imback.palpal"

    /** 已验证的目标版本；不匹配时不注入，只打日志 */
    const val SUPPORTED_VERSION = "2.58.2"

    /** 配置文件（模块侧 SharedPreferences 文件名） */
    const val PREFS_NAME = "ytvip_config"

    /**
     * 伪造的 VIP 剩余天数。
     * 注意：vip_expire 是 **32 位 int** 秒级时间戳，上限 2147483647（2038-01-19）。
     * 当前时间约 1.78e9，只有约 3.6e8 秒余量，因此不要超过 1000 天，否则溢出成负数。
     */
    const val VIP_DAYS = 365

    // ---------------- 配置键 ----------------
    object K {
        const val MASTER = "master"

        // 各 hook 单元开关
        const val H_USER_INFO = "h_user_info"
        const val H_MANAGER = "h_manager"
        const val H_VISITOR = "h_visitor"
        const val H_GREET = "h_greet"
        const val H_VIPINFO = "h_vipinfo"
        const val H_DUMP = "h_dump"

        // 行为参数
        const val UNLOCK_COUNT = "unlock_count"
        const val GREET_COUNT = "greet_count"
    }

    // ---------------- 目标类坐标 ----------------
    object T {
        /** VIP 实体：is_vip / vip_expire / noble_* */
        const val USER_INFO = "com.imback.commonbase.entity.UserInfo"

        /** VIP 统一出口：C() / E() / D() / F() / G() */
        const val USER_INFO_MANAGER = "com.imback.commonbase.utils.UserInfoManager"

        /** 访客页状态（isVip / freeUnlockSize / maxAvailableFreeUnlockIndex） */
        const val VISITOR_PAGE_STATE = "ih.UserVisitorPageState"

        /** 打招呼次数引导 */
        const val GREET_GUIDE = "Bb.GreetLimitChargeVipGuide"

        /** 套餐信息：isVip / vipExpireTime / m() */
        const val VIP_CHARGE_INFO = "Bb.VipChargeInfo"

        /** 权益表格项：code / vipTableValue / name */
        const val VIP_PRIVILEGE_ITEM = "Bb.VipPrivilegeTableItemInfo"
    }

    // ---------------- 混淆字段名（注解缺失时的兜底） ----------------
    object F {
        const val IS_VIP = "f76016E"
        const val VIP_EXPIRE = "f76017F"
        const val HIDE_VISIT = "f76015D"
    }

    /** 注解里出现的 JSON key，用于动态定位字段 */
    object A {
        const val IS_VIP = "is_vip"
        const val VIP_EXPIRE = "vip_expire"
        const val HIDE = "hide"
    }
}
