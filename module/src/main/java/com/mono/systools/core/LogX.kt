package com.mono.systools.core

import de.robv.android.xposed.XposedBridge

/**
 * 双通道日志：
 *  - 注入到目标进程时，走 XposedBridge.log（LSPosed 里可见）
 *  - 模块自身进程（设置界面）里 XposedBridge 不可用，静默降级到 Logcat
 */
object LogX {

    private const val LOGCAT_TAG = Cfg.TAG

    @Volatile
    var enabled: Boolean = true

    fun i(msg: String) = write("I", msg)
    fun w(msg: String) = write("W", msg)
    fun e(msg: String, t: Throwable? = null) {
        write("E", msg + (t?.let { " :: ${it.javaClass.simpleName}: ${it.message}" } ?: ""))
    }

    private fun write(level: String, msg: String) {
        if (!enabled) return
        val line = "${Cfg.TAG}/$level $msg"
        runCatching { XposedBridge.log(line) }
            .onFailure {
                runCatching {
                    when (level) {
                        "E" -> android.util.Log.e(LOGCAT_TAG, msg)
                        "W" -> android.util.Log.w(LOGCAT_TAG, msg)
                        else -> android.util.Log.i(LOGCAT_TAG, msg)
                    }
                }
            }
    }
}
