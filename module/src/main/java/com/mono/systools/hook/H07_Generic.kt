package com.mono.systools.hook

import com.mono.systools.core.Cfg
import com.mono.systools.core.LogX
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * H07 · VIP 语义勘察器（只观察，不改写）
 *
 * 为什么不做"盲翻单字母 boolean"：
 *   2.58.2 真机 dump 后逐方法反编译核实，UserInfo 的 12 个大写单字母方法里
 *   只有 K() 是 VIP 语义，其余是身份/系统号判定，盲翻会把当前用户变成
 *   "官方客服""系统号 999""未登录"，比原状态更糟：
 *
 *     UserInfo.A()  → 官方号/贵族客服/官方团队/房间通知      ✗ 别动
 *     UserInfo.C()  → f76047j.f75905b == 999（系统号）        ✗ 别动
 *     UserInfo.D()  → 房间互动通知                            ✗ 别动
 *     UserInfo.G()  → userId.equals("1000")（官方号）          ✗ 别动
 *     UserInfo.E()  → !p() && f76052o                         ? 未定性
 *     UserInfo.F()  → f76024M == 1                            ? 未定性
 *     UserInfo.K()  → is_vip == 1                             ✅ H01 已接管
 *     UserInfo.H/I/J/L/N → 已过期/即将到期                     ✅ 保持 false（H01）
 *
 *     UserInfoManager.A() → 转发 UserInfo.A()（官方号）        ✗ 别动
 *     UserInfoManager.B() → 转发 UserInfo.F()                 ✗ 别动
 *     UserInfoManager.C() → is_vip 出口，0/1                  ✅ H02 已接管
 *     UserInfoManager.D/E/F/G → VIP 派生状态                   ✅ H02 已接管
 *     UserInfoManager.H() → w()==null || 昵称为空（未登录态）  ✗ 绝不能动
 *
 * 于是本单元退化为纯诊断件：把方法清单和带 vip/noble 注解的字段打出来，
 * 供后续版本变化时重新核对语义。零副作用。
 *
 * 真正有效的覆盖面来自 H01 + H02 —— 它们卡在 is_vip 字段与 C() 出口上，
 * 是全 App 会员状态的唯一上游。
 */
object H07Generic {

    fun install(cl: ClassLoader) {
        val userInfo = HookUtil.clazz(Cfg.T.USER_INFO, cl)
        val manager = HookUtil.clazz(Cfg.T.USER_INFO_MANAGER, cl)

        if (userInfo == null && manager == null) {
            LogX.w("H07 两个源头类都找不到，跳过")
            return
        }

        userInfo?.let {
            dumpMethods("UserInfo", it)
            scanAnnotatedFields(it)
        }
        manager?.let {
            dumpMethods("UserInfoManager", it)
        }

        LogX.i("H07 勘察完成（只观察，不改写）")
    }

    /** 打印全部无参 public 方法（分批，避开 logcat 单行长度限制） */
    private fun dumpMethods(tag: String, clazz: Class<*>) = runCatching {
        val list = clazz.declaredMethods
            .filter { it.parameterCount == 0 && Modifier.isPublic(it.modifiers) }
            .map { "${it.name}:${it.returnType.simpleName}" }
            .sorted()
        LogX.i("[dump] $tag 共 ${list.size} 个无参 public 方法")
        list.chunked(12).forEachIndexed { i, batch ->
            LogX.i("[dump] $tag#$i ${batch.joinToString(", ")}")
        }
    }.onFailure { LogX.w("dump $tag 失败：${it.message}") }

    /**
     * 打印注解含 vip / member / noble 的字段。
     * 这些是 JSON key 语义的稳定锚点（抗混淆），比字段名 f76016E 可靠。
     */
    private fun scanAnnotatedFields(clazz: Class<*>) = runCatching {
        val found = clazz.declaredFields.mapNotNull { f: Field ->
            val ann = f.annotations.joinToString(" ") { a ->
                runCatching { a.toString() }.getOrDefault("")
            }
            when {
                ann.contains("vip", true) || ann.contains("member", true) -> "${f.name}(${f.type.simpleName})=$ann"
                ann.contains("noble", true) -> "${f.name}(${f.type.simpleName})=$ann"
                else -> null
            }
        }
        LogX.i("UserInfo 注解命中字段 ${found.size} 个：")
        found.forEach { LogX.i("  · $it") }
    }.onFailure { LogX.w("scanAnnotatedFields 失败：${it.message}") }
}
