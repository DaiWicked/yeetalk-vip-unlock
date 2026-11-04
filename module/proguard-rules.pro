# Xposed 入口必须保留
-keep class com.lcs.yeetalk.HookEntry { *; }
-keep class com.lcs.yeetalk.hook.** { *; }

# 反射引用的目标类不做处理（运行时位于目标 App 内，这里只是防误删字符串）
-keepnames class * { *; }

-dontwarn de.robv.android.xposed.**
-keep class de.robv.android.xposed.** { *; }
