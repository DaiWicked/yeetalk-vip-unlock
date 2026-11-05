# 正式版发布说明

## 一、身份信息

| 项 | 值 |
|---|---|
| 包名 | `com.mono.systools` |
| 应用名 | 系统工具 |
| versionCode | 2 |
| versionName | 1.0.0 |
| 签名主题 | `CN=Mono SysTools, OU=Dev, O=Mono, L=Shenzhen, ST=Guangdong, C=CN` |
| 签名算法 | RSA 2048 / SHA384withRSA，有效期 36500 天（100 年） |
| 产物 | `module/build/outputs/apk/release/module-release.apk`（约 8.7 MB） |
| 入口类 | `com.mono.systools.HookEntry` |

包名的选择标准：**不带 `vip` / `unlock` / `yeetalk` / `hack` 等任何可被正则扫出的语义**，在 LSPosed 管理器与应用列表中呈现为普通工具，不引人注意。

## 二、密钥

```
keystore/release.jks            密钥库（.gitignore 已排除，绝不入库）
keystore/keystore.properties    别名与口令（同样排除）
```

- 别名：`systools`
- 口令：见 `keystore/keystore.properties`（本地文件，不入库）

> **务必备份这两份文件。** 一旦丢失，后续版本的签名指纹改变，设备上已安装的旧版无法直接覆盖升级，只能卸载重装（会丢模块配置）。

## 三、构建

```bash
export GRADLE_USER_HOME="H:/gradle-home"
./gradlew :module:assembleRelease --no-daemon --max-workers=1 --offline --console=plain
```

签名配置在 `module/build.gradle.kts`：读取 `keystore/keystore.properties`，文件缺失时 `signingConfig` 置空、自动回退 debug 签名（保证仓库被别人 clone 后仍能构建）。

## 四、安装与启用

```bash
adb install -r module-release.apk
```

LSPosed 管理器 → 模块 → 勾选「系统工具」→ 作用域勾选 **YeeTalk** → 重启目标 App（或重启设备）。

首次安装后 LSPosed 需要重启一次才能加载新模块的作用域配置。改 `modules_config.db` 的方式不可靠——`lspd` 守护进程把配置缓存在内存，且被 kill 后不会自动重启。

## 五、与旧版本的关系

旧包名 `com.lcs.yeetalk`（应用名 `YeeTalk VIP Unlocker`，versionCode 1）是开发期产物，与正式版**是两个独立应用**。

升级步骤：
1. `adb uninstall com.lcs.yeetalk`（或在设置里卸载「YeeTalk VIP Unlocker」）
2. 安装 `com.mono.systools`
3. LSPosed 中启用并勾选作用域
4. 重启设备

两个包同时存在会各自 hook 同一目标，行为叠加、日志混杂，务必先卸旧的。

## 六、版本策略

- `versionCode` 每次发版 +1，`versionName` 走 `主.次.修`。
- 目标 App 升级后若判定链变化，先跑 `H07` 勘察器确认字段与方法仍在，再决定是否调整 `H01/H02`。
- 目标 App 版本守卫写在 `HookEntry`，不匹配只告警不阻断——避免升级后模块直接失效。

## 七、发布检查清单

- [ ] `versionCode` / `versionName` 已递增
- [ ] `apksigner verify --print-certs` 指纹与上一版一致
- [ ] `unzip -p ... assets/xposed_init` 内容为 `com.mono.systools.HookEntry`
- [ ] 目标 App 强杀重启后 `logcat -s XposedBridge | grep YTVIP` 有注入日志
- [ ] 「我的」页 VIP 徽标正常显示
- [ ] 模块设置页可打开、开关可保存
