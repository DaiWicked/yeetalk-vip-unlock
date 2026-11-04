package com.lcs.yeetalk.core

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences

/**
 * 配置读取，两条路径：
 *  - 目标进程内：XSharedPreferences（LSPosed 会重定向到模块的 prefs 文件）
 *  - 模块自身进程：普通 SharedPreferences
 */
object Prefs {

    @Volatile
    private var remote: XSharedPreferences? = null

    @Volatile
    private var local: SharedPreferences? = null

    /** 在目标 App 进程里调用 */
    fun initRemote() {
        if (remote != null) return
        remote = runCatching {
            XSharedPreferences(Cfg.MODULE_PKG, Cfg.PREFS_NAME).also {
                // 旧版 Xposed 需要；LSPosed 上无副作用
                runCatching { it.makeWorldReadable() }
                runCatching { it.reload() }
            }
        }.onFailure { LogX.w("XSharedPreferences 初始化失败：${it.message}") }
            .getOrNull()
    }

    /** 在模块自身界面进程里调用 */
    fun initLocal(ctx: Context) {
        if (local == null) {
            local = ctx.applicationContext.getSharedPreferences(Cfg.PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // XSharedPreferences 并不实现 SharedPreferences 接口，两者只能分别取值，
    // 不能先 `remote ?: local` 合成一个公共类型（那样会推成 Any 而编译失败）。
    fun bool(key: String, def: Boolean): Boolean = runCatching {
        remote?.getBoolean(key, def) ?: local?.getBoolean(key, def) ?: def
    }.getOrDefault(def)

    fun int(key: String, def: Int): Int = runCatching {
        remote?.getInt(key, def) ?: local?.getInt(key, def) ?: def
    }.getOrDefault(def)

    fun str(key: String, def: String): String = runCatching {
        remote?.getString(key, def) ?: local?.getString(key, def) ?: def
    }.getOrDefault(def)

    /** 总开关（默认开） */
    fun master(): Boolean = bool(Cfg.K.MASTER, true)

    /** 单元开关：总开关关闭时一律 false */
    fun unit(key: String): Boolean = master() && bool(key, true)
}
