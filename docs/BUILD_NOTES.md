# 构建踩坑记录（2.58.2 / 本机实测）

都是真实报错 + 真实修法，下次换机器或升级版本时会再遇上。

## 1. 路径含中文直接被 AGP 拒绝

```
Your project path contains non-ASCII characters. This will most likely cause the build to fail on Windows.
```

仓库在 `H:\yeetalk破解`。在 `gradle.properties` 加：

```properties
android.overridePathCheck=true
```

这是 AGP 的实验开关，会打一条 WARNING，无害。要静音再加
`android.sync.suppressAgpWarnings=UNSUPPORTED_PROJECT_OPTION_USE`。

## 2. AGP 9 内置 Kotlin，别再应用 kotlin-android

```
The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.
```

AGP 9.x 自带 Kotlin 支持。根 `build.gradle.kts` 与模块里都不要 `alias(libs.plugins.kotlin.android)`，
只保留 `kotlin.plugin.compose`（Compose 编译器插件仍需要）。

残留症状：根脚本 `alias(libs.plugins.jetbrains.compose)` 会报 `Unresolved reference 'jetbrains'`
（该插件已从 `libs.versions.toml` 移除时）。

## 3. Compose 运行时版本必须与 backdrop 对齐

`io.github.kyant0:backdrop:2.0.1` 是以 **Compose Multiplatform 1.12.0** 编译的，
它以 `implementation` 引入 Compose，运行时会把对应的 androidx compose 带进来。
如果宿主工程用更老的 Compose（如 1.7.x）编译，运行期就是 `NoSuchMethodError`。

本仓库对齐结果（全部命中本机缓存，零下载）：

```
agp 9.3.3 · kotlin 2.4.20 · androidx.compose 1.12.1 · material3 1.5.0-alpha29 · backdrop 2.0.1
```

## 4. material3 alpha 的传递依赖版本要钉住

```
Could not resolve androidx.compose.material:material-ripple:1.12.0
```

`material3:1.5.0-alpha29` 传递依赖 `material-ripple:1.12.0`，而缓存里只有 `1.12.1`。
在 `libs.versions.toml` 显式声明 1.12.1（补丁版，API 兼容）即可：

```toml
compose-material-ripple = { group = "androidx.compose.material", name = "material-ripple", version.ref = "composeAndroid" }
```

## 5. Xposed API 走本地 jar，不走远程仓库

`de.robv.android.xposed:api:82` 只发布在 `api.xposed.info`，该站点已不稳定（HTTPS 超时）。
仓库内自带 `libs/xposed-api-82.jar`（25 KB）：

```kotlin
compileOnly(files("../libs/xposed-api-82.jar"))
```

`compileOnly` 保证它**不进 APK**，运行时由 LSPosed 注入真实实现。

## 6. 低配构建模式（重要）

本机曾因内存 + C 盘空间双重挤压蓝屏。`gradle.properties` 已固定为低配：

```properties
org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=false
org.gradle.workers.max=2
org.gradle.vfs.watch=false
kotlin.daemon.jvmargs=-Xmx1024m
```

构建命令：

```bash
export GRADLE_USER_HOME="H:/gradle-home"   # 缓存落在数据盘，不写 C 盘
./gradlew :module:assembleDebug --no-daemon --max-workers=1 --offline
```

`--no-daemon` + `--max-workers=1` 峰值内存压在 2 G 以内；`--offline` 避免网络抖动拖死构建。
**不要**加 `parallel=true` 或把堆调到 3 G 以上。

## 7. 蓝屏后的缓存修复

强制断电会截断 Gradle 缓存，典型报错：

```
Unexpected lock protocol found in lock file. Expected 3, found 0.
cache fileHashes.bin is corrupt. Discarding.
No cached version of X available for offline mode.
```

修复顺序（从轻到重）：

1. 删 `$GRADLE_USER_HOME/**/*.lock` 中 0 字节的文件
2. 删 `caches/9.8.0/fileHashes`、`caches/journal-1`、`.tmp`
3. 元数据索引坏了再删 `caches/modules-2/metadata-*`
   ⚠️ 删掉后若网络不通，Gradle 会要求重新联网校验 POM —— 此时从另一份完好缓存
   （如原 `C:\Users\<user>\.gradle\caches\modules-2\metadata-*`）复制回来即可

## 8. 已通过的编译结果

```
BUILD SUCCESSFUL in 1m 35s
module/build/outputs/apk/debug/module-debug.apk   13 MB
  ├─ assets/xposed_init → com.lcs.yeetalk.HookEntry
  └─ classes.dex
```

尚未验证：装机注入（需要 Magisk + LSPosed 设备）、backdrop 玻璃效果的实机渲染。
