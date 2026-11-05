package com.lcs.yeetalk.hook

import com.lcs.yeetalk.core.Cfg
import com.lcs.yeetalk.core.LogX
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field

/**
 * hook 公共设施：类定位、字段动态发现、方法翻转、构造后注入。
 *
 * 设计原则（对应 docs/PROJECT_PLAN.md 执行纪律）：
 *  - 任何一步失败都只 log 不抛；单个单元失败不影响其他单元。
 *  - 字段优先按注解值定位（抗混淆），失败再按混淆名兜底。
 */
object HookUtil {

    /** 类加载失败返回 null，不抛 */
    fun clazz(name: String, cl: ClassLoader): Class<*>? =
        runCatching { XposedHelpers.findClass(name, cl) }
            .onFailure { LogX.w("找不到类 $name") }
            .getOrNull()

    /**
     * 按注解内容找字段。
     * 目标实体用 `@InterfaceC6403c("is_vip")` 这类自定义注解标注 JSON key，
     * 直接看注解 toString 里是否含关键串即可，无需知道注解类名。
     */
    fun fieldByAnnotation(clazz: Class<*>, hint: String): Field? =
        clazz.declaredFields.firstOrNull { f ->
            f.annotations.any { a ->
                runCatching { a.toString().contains(hint) }.getOrDefault(false)
            }
        }

    /** 按混淆名找字段（沿父类链） */
    fun fieldByName(clazz: Class<*>, name: String): Field? {
        var c: Class<*>? = clazz
        while (c != null) {
            c.declaredFields.firstOrNull { it.name == name }?.let { return it.apply { isAccessible = true } }
            c = c.superclass
        }
        return null
    }

    /** 注解优先 + 名字兜底 */
    fun resolveField(clazz: Class<*>, annotationHint: String, fallbackName: String): Field? =
        (fieldByAnnotation(clazz, annotationHint) ?: fieldByName(clazz, fallbackName))
            ?.apply { isAccessible = true }

    fun setInt(target: Any, f: Field?, value: Int) {
        if (f == null) return
        runCatching { f.setInt(target, value) }
            .onFailure { LogX.w("setInt ${f.name} 失败：${it.message}") }
    }

    fun getInt(target: Any, f: Field?): Int? =
        f?.let { runCatching { it.getInt(target) }.getOrNull() }

    /** 把类里所有同名 boolean 方法（含重载）的结果强制为 [value] */
    fun hookBool(clazz: Class<*>, name: String, value: Boolean): Boolean = runCatching {
        var count = 0
        XposedBridge.hookAllMethods(clazz, name, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                param.result = value
            }
        }).also { count = it.size }
        LogX.i("hook ${clazz.simpleName}.$name → $value (${count} 处)")
        count > 0
    }.onFailure { LogX.w("hook ${clazz.simpleName}.$name 失败：${it.message}") }.getOrDefault(false)

    /** 把类里所有同名 int 方法的结果强制为 [value] */
    fun hookInt(clazz: Class<*>, name: String, value: Int): Boolean = runCatching {
        var count = 0
        XposedBridge.hookAllMethods(clazz, name, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                param.result = value
            }
        }).also { count = it.size }
        LogX.i("hook ${clazz.simpleName}.$name → $value (${count} 处)")
        count > 0
    }.onFailure { LogX.w("hook ${clazz.simpleName}.$name 失败：${it.message}") }.getOrDefault(false)

    /**
     * 带调用栈追踪的 hook：正常改写返回值，同时每个方法只打印前 [limit] 次命中及调用栈。
     * 用途：定位"到底是谁在读这个判定"——确认 UI 数据源是否真的走这里。
     */
    fun hookTraced(clazz: Class<*>, name: String, value: Any?, limit: Int = 3): Boolean = runCatching {
        var count = 0
        XposedBridge.hookAllMethods(clazz, name, object : XC_MethodHook() {
            private var n = 0
            override fun afterHookedMethod(param: MethodHookParam) {
                param.result = value
                if (n++ < limit) {
                    val st = android.util.Log.getStackTraceString(Throwable())
                        .lineSequence().drop(1).take(12).joinToString("\n")
                    LogX.i("${clazz.simpleName}.$name() 命中#$n → $value\n$st")
                }
            }
        }).also { count = it.size }
        LogX.i("traced ${clazz.simpleName}.$name → $value (${count} 处)")
        count > 0
    }.onFailure { LogX.w("traced ${clazz.simpleName}.$name 失败：${it.message}") }.getOrDefault(false)

    /**
     * 把所有同名方法的结果改成"真值"，返回类型自适应。
     * 用于返回类型不确定的混淆方法（例如 UserInfoManager.D/E/F/G 可能是 boolean 也可能是 int）。
     */
    fun hookTruthy(clazz: Class<*>, name: String): Boolean = runCatching {
        var count = 0
        XposedBridge.hookAllMethods(clazz, name, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // param.method 的静态类型是 java.lang.reflect.Member，需转 Method 才拿得到 returnType
                val rt = (param.method as? java.lang.reflect.Method)?.returnType ?: Unit::class.java
                param.result = truthyOf(rt)
            }
        }).also { count = it.size }
        LogX.i("hook ${clazz.simpleName}.$name → truthy (${count} 处)")
        count > 0
    }.onFailure { LogX.w("hook ${clazz.simpleName}.$name 失败：${it.message}") }.getOrDefault(false)

    private fun truthyOf(t: Class<*>): Any = when {
        t == Boolean::class.javaPrimitiveType || t == Boolean::class.javaObjectType -> true
        t == Int::class.javaPrimitiveType || t == Int::class.javaObjectType -> 1
        t == Long::class.javaPrimitiveType || t == Long::class.javaObjectType -> 1L
        t == Float::class.javaPrimitiveType || t == Float::class.javaObjectType -> 1f
        t == Double::class.javaPrimitiveType || t == Double::class.javaObjectType -> 1.0
        t == String::class.java -> "1"
        else -> true
    }

    /** 所有构造完成后回调（用于字段注入） */
    fun afterAllConstructors(clazz: Class<*>, block: (Any) -> Unit) {
        runCatching {
            XposedBridge.hookAllConstructors(clazz, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.thisObject?.let { runCatching { block(it) } }
                }
            })
        }.onFailure { LogX.w("hookAllConstructors ${clazz.simpleName} 失败：${it.message}") }
    }

    /** 所有方法（观察某个方法被调用时打印，用于 dump） */
    fun observe(clazz: Class<*>, name: String, block: (XC_MethodHook.MethodHookParam) -> Unit) {
        runCatching {
            XposedBridge.hookAllMethods(clazz, name, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    runCatching { block(param) }
                }
            })
        }.onFailure { LogX.w("observe ${clazz.simpleName}.$name 失败：${it.message}") }
    }

    fun nowSec(): Int = (System.currentTimeMillis() / 1000L).toInt()

    fun vipExpireSec(): Int = nowSec() + Cfg.VIP_DAYS * 86400
}
