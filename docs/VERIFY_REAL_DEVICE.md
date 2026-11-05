# 真机验证记录（2026-11-05）

目标 App：`com.imback.palpal` YeeTalk 2.58.2 (versionCode 1955)
> 注：本文记录均基于开发期包名 `com.lcs.yeetalk`。正式版改名 `com.mono.systools` 后逻辑不变，仅日志前缀变化。

模块：`com.lcs.yeetalk`（debug APK，13 MB）

## 1. 测试环境

| 项 | 值 |
| --- | --- |
| 设备 | 24053PY09C（`chenfeng`），序列号 `c08998dc` |
| 系统 | Android 16 / API 36 / arm64-v8a / HyperOS V816 |
| Root | KernelSU（`su` context `u:r:ksu:s0`），容器内另有 `zygisksu`、`susfs4ksu_unified`、`tricky_store` |
| 框架 | LSPosed（`/data/adb/lspd`），模块配置库 `config/modules_config.db` |
| adb | `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` 1.0.41 / 37.0.1（**不在 PATH**，需全路径调用） |

## 2. LSPosed 侧配置（已确认）

```
modules:       com.lcs.yeetalk → /data/app/~~nvuFsPZZHhb3fEqxo8axIA==/com.lcs.yeetalk-cmyih__PInUiusI1HJIiXw==/base.apk
modules_state: com.lcs.yeetalk | user 0 | enabled = 1
scope:         com.lcs.yeetalk → com.imback.palpal | user 0
```

安装方式：`adb install -r module/build/outputs/apk/debug/module-debug.apk` → Success。
改模块后必须 `am force-stop com.imback.palpal` 再启动，新代码才进进程。

> 注：手机端没有 `sqlite3`。要改配置库时用 `platform-tools/sqlite3.exe`，
> 但**该二进制打不开含中文的路径**，db 必须先落到纯 ASCII 目录（本次用 `H:\lspdwork\`）。
> 拉库时 `-wal` / `-shm` 三个文件要一起拉，改完 `PRAGMA wal_checkpoint(TRUNCATE)` 再推回。

## 3. 注入日志（实测）

```
YTVIP/I ========== [YTVIP] 开始注入 ==========
YTVIP/I 目标：com.imback.palpal  version=null  期望=2.58.2  process=com.imback.palpal
YTVIP/I H01 字段定位：is_vip=E, vip_expire=F
YTVIP/I hook UserInfo.K → true (1 处)
YTVIP/I hook UserInfo.H → false (1 处)
YTVIP/I hook UserInfo.I / J / L / N → false (各 1 处)
YTVIP/I H01 已装载
YTVIP/I hook UserInfoManager.C → 1 (1 处)
YTVIP/I hook UserInfoManager.E / D / F / G → truthy (各 1 处)
YTVIP/I H02 已装载
YTVIP/W 找不到类 ih.UserVisitorPageState
YTVIP/W 找不到类 Bb.GreetLimitChargeVipGuide
YTVIP/W 找不到类 Bb.VipChargeInfo
YTVIP/W 找不到类 Bb.VipPrivilegeTableItemInfo
YTVIP/I ========== [YTVIP] 注入结束 ==========
```

运行期命中（`hookTraced` 埋点）：

```
UserInfo.K() 命中#3 → true
UserInfoManager.C() 命中#2 → 1
H01 构造注入#3：E=1 F=1825415092      ← vip_expire = 2027-11-05
```

## 4. 实测结论

| 验证点 | 结果 | 证据 |
| --- | --- | --- |
| 模块被 LSPosed 加载进目标进程 | ✅ | 日志 tag `LSPosedFramework` 前缀含 `(com.imback.palpal)[com.lcs.yeetalk,...]` |
| `UserInfo.K()` 被调用并被改写 | ✅ | 命中 ≥3 次，返回 true |
| `UserInfoManager.C()` 被调用并被改写 | ✅ | 命中 ≥2 次，返回 1 |
| `UserInfo` 构造注入 | ✅ | 每次 new 后 `E=1`、`F=1825415092` |
| **个人主页 VIP 徽标** | ✅ **由无变有** | 注入前"Jarvis Yang"，注入后"Jarvis Yang 🏅VIP" |
| VIP 购买页状态位（"已开通/剩余N天"） | ❌ 未变 | 该页由服务端订单接口驱动，本地 `UserInfo` 不参与 |
| **VIP 更换主页封面** | ✅ **可换且已生效** | 重新登录后实测：封面从默认 YeeTalk 蓝底图案换成自定义图片，入口未被拦截（提审是平台内容审核流程，与权限无关） |

**核心结论：本地 VIP 状态链路（`UserInfo.E` 字段 + `K()` + `C()`）已完全打通，凡是读本地
`UserInfo` / `UserInfoManager` 的渲染点都会显示 VIP。购买页这类"服务端订单数据"驱动的界面
属于 C 层天花板，本地 hook 覆盖不到。**

### 4.1 VIP 权益实测记录

| 权益 | 结果 | 说明 |
| --- | --- | --- |
| 个人主页 VIP 徽标 | ✅ | 名字后金色 VIP 标，登录态重建后依然生效 |
| **自定义主页封面** | ✅ | 可进入编辑并成功更换，说明本地权限校验已通过 |
| 换封面的"审核" | ⏳ | 这是内容审核流程，与 VIP 权限无关，不影响权益解锁判定 |
| VIP 购买页状态位 | ❌ | 服务端订单数据驱动，本地改不动 |
| **翻译（客户端门控）** | ✅ | 点消息右下角「AI」翻译，日志 `UserInfo.K() 命中#35/#36`、`UserInfoManager.C() 命中#4/#5`，证明客户端翻译前会读 VIP 状态，且已被改写为 true |
| 翻译次数配额 | ❌ 服务端 | `MsgFreeTranslateLackProvider` 只渲染服务端下发的 `MsgInfo`（`Hb.FreeTranslateNotEnoughTipMsg.content` 即服务端文案），配额按账号在服务端计算，本地无法改动 |

## 5. 两个实测发现（与静态分析不符，已修正认知）

1. **字段真名是 `E` / `F`，不是 jadx 里看到的 `f76016E` / `f76017F`。**
   `resolveField` 靠 `@InterfaceC6403c("is_vip")` 注解兜底才找到，说明字段名猜测不可靠，
   **注解名（`is_vip` / `vip_expire`）才是稳定锚点**。后续所有字段定位都应走注解。
2. **四个业务类运行时 `loadClass` 失败**：`Bb.VipChargeInfo`、`Bb.VipPrivilegeTableItemInfo`、
   `Bb.GreetLimitChargeVipGuide`、`ih.UserVisitorPageState`。
   已确认 `VipChargeInfo` 字符串存在 `classes4.dex`（2 次），但搜不到 `LBb/VipChargeInfo;`
   形式的类描述符 —— 说明 R8 把类名本身混淆成了单字母，`Bb/VipChargeInfo` 只是 jadx 的
   可读别名。**要在运行时定位它们，只能靠方法签名/字段特征反查，不能靠名字。**

## 6. 复现步骤

```bash
cd "H:/yeetalk破解"; export GRADLE_USER_HOME="H:/gradle-home"
./gradlew :module:assembleDebug --no-daemon --max-workers=1 --offline --console=plain

ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
"$ADB" install -r module/build/outputs/apk/debug/module-debug.apk
"$ADB" shell "am force-stop com.imback.palpal"
"$ADB" logcat -c
"$ADB" shell "am start -n com.imback.palpal/com.imback.palpal.splash.SplashActivity"
sleep 8
"$ADB" logcat -d | grep -a YTVIP
```
