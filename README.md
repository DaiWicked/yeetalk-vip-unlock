# 系统工具 · LSPosed Module

面向 **YeeTalk 2.58.2**（`com.imback.palpal`，versionCode 1955）的 LSPosed 模块：在运行期把目标 App 的**本地 VIP 状态**翻转为"已开通"。

UI 采用 **Liquid Glass（玻璃拟态）** 风格，底层复用 [Kyant0/Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)（Apache-2.0）。

---

## 实测结论（真机验证完成）

| 能力 | 结果 | 依据 |
| --- | --- | --- |
| 个人主页 VIP 徽标 | ✅ | 注入前 `Jarvis Yang`，注入后 `Jarvis Yang 🏅VIP` |
| 自定义主页封面 | ✅ | 成功更换并生效（提审属内容审核，与权限无关） |
| 翻译功能客户端门控 | ✅ | 点翻译时日志 `UserInfo.K() 命中#35`、`UserInfoManager.C() 命中#4` |
| VIP 购买页状态位 | ❌ | 服务端订单接口驱动，本地 `UserInfo` 不参与 |
| 翻译次数配额 | ❌ | 服务端按账号计次，`MsgFreeTranslateLackProvider` 只渲染服务端下发的提示消息 |

**边界说明**：本模块只能改变**客户端读到本地状态后如何渲染**。任何由服务端下发的配额、订单、鉴权结果都不受影响。

---

## 状态

| 阶段 | 内容 | 状态 |
|---|---|---|
| P0 | 环境 / 样本解包 / Manifest | ✅ |
| P1 | 关键坐标定位（判定链、存储、权益结构） | ✅ |
| P2 | 模块骨架 + Compose 玻璃 UI | ✅ 编译通过 |
| P3 | 真机验证 | ✅ 见 [docs/VERIFY_REAL_DEVICE.md](docs/VERIFY_REAL_DEVICE.md) |
| P4 | release 打包 | ⬜ 目前产物是 debug APK |

> 本仓库**只含源码**。样本 APK、反编译产物、工具链均在 `artifacts/`（已 git 忽略），说明见 [docs/ARTIFACTS.md](docs/ARTIFACTS.md)。

---

## Hook 单元

| 文件 | 目标 | 作用 | 实测 |
|---|---|---|---|
| `H01_UserInfo.kt` | `UserInfo.E/F`（`is_vip`/`vip_expire` 注解定位）+ `K/H/I/J/L/N` | 构造注入 + `K()→true`，`H/I/J/L/N→false`（总闸） | ✅ 生效 |
| `H02_Manager.kt` | `UserInfoManager.C/E/D/F/G` | 统一出口 `C()→1`，兜住不走 `UserInfo` 的页面 | ✅ 生效 |
| `H03_Visitor.kt` | `ih.UserVisitorPageState` | 解锁额度放大 | ⚠️ 类名被 R8 混淆，未定位 |
| `H04_Greet.kt` | `Bb.GreetLimitChargeVipGuide` | 打招呼次数（dump 签名 + 白名单） | ⚠️ 类名被 R8 混淆，未定位 |
| `H05_VipInfo.kt` | `Bb.VipChargeInfo` | 套餐页"已开通" | ⚠️ 类名被 R8 混淆，未定位 |
| `H06_Dump.kt` | `Bb.VipPrivilegeTableItemInfo` | 只读 dump 权益全表 | ⚠️ 同上 |
| `H07_Generic.kt` | `UserInfo` / `UserInfoManager` | **纯勘察**：dump 方法清单与注解字段，零副作用 | ✅ 生效 |

**为什么 H03–H06 定位不到**：`VipChargeInfo` 字符串确实存在于 `classes4.dex`，但搜不到 `LBb/VipChargeInfo;` 形式的类描述符——R8 把类名本身混淆成了单字母，`Bb/VipChargeInfo` 只是 jadx 给的可读别名。运行时按名字 `loadClass` 必然失败，只能靠方法签名反查。

**为什么不做"全量翻转单字母方法"**：`UserInfo` 的 12 个大写单字母方法里只有 `K()` 是 VIP 语义，其余是身份判定——盲翻会把当前用户变成"官方客服/系统号 999/未登录"，实测导致登录态被服务端踢掉。完整语义表见 [docs/HOOK_SEMANTICS.md](docs/HOOK_SEMANTICS.md)。

每个单元独立开关、独立 try/catch；`HookEntry` 带包名守卫与版本告警，单元失败只打日志，不影响目标 App 启动。

---

## 成员插件使用

1. 安装 `module-debug.apk`
2. LSPosed 管理器中启用模块，作用域勾选 **YeeTalk (`com.imback.palpal`)**
3. 强杀 YeeTalk 并重启（`am force-stop` 后等 2 秒再 `am start`，否则可能拉不起来）
4. 验证注入：
   ```bash
   adb logcat -s XposedBridge | grep YTVIP
   ```
   应出现 `========== [YTVIP] 开始注入 ==========`、`H01 字段定位：is_vip=E, vip_expire=F`、`traced UserInfo.K → true`

> 启动目标 App 用显式 Activity：`am start -n com.imback.palpal/com.imback.palpal.splash.SplashActivity`。
> `monkey -p ... LAUNCHER 1` 拉不起来（进程为空）。

---

## 构建

```bash
export GRADLE_USER_HOME="H:/gradle-home"     # 缓存放数据盘，C 盘空间不足会蓝屏
./gradlew :module:assembleDebug --no-daemon --max-workers=1 --offline --console=plain
```

产物：`module/build/outputs/apk/debug/module-debug.apk`（约 13 MB）

**版本基线**（全部选用本机已缓存的版本，零下载）：
Gradle 9.8.0 · AGP 9.3.3 · Kotlin 2.4.20 · Jetpack Compose 1.12.1 · material3 1.5.0-alpha29 · Backdrop 2.0.1

> AGP 9 已内置 Kotlin 支持，**不再需要** `org.jetbrains.kotlin.android` 插件。
> 路径含中文时必须 `android.overridePathCheck=true`。详见 [docs/BUILD_NOTES.md](docs/BUILD_NOTES.md)。

---

## 目录

```
.
├── docs/
│   ├── PROJECT_PLAN.md              项目清单：坐标表、里程碑、权益分层
│   ├── HOOK_SEMANTICS.md            UserInfo 判定族语义对照表（改 hook 前必读）
│   ├── VERIFY_REAL_DEVICE.md        真机验证记录与结论
│   ├── BUILD_NOTES.md               构建踩坑与参数
│   ├── UI_DESIGN.md                 Liquid Glass UI 复用方案
│   ├── ARTIFACTS.md                 本地产物清单（不入库）
│   └── images/                      验证截图
├── module/
│   └── src/main/
│       ├── assets/xposed_init       Xposed 入口声明
│       ├── res/                     字符串 / 主题 / xposed_scope
│       └── java/com/lcs/yeetalk/
│           ├── HookEntry.kt         IXposedHookLoadPackage 分派
│           ├── MainActivity.kt      Compose 设置页宿主
│           ├── core/                Cfg / LogX / Prefs
│           ├── hook/                H01–H07 各 hook 单元
│           └── ui/                  玻璃组件与设置界面
├── libs/xposed-api-82.jar           compileOnly，不进 APK
└── artifacts/                       ← git 忽略（样本 + 反编译 + 工具）
```

---

## 免责

仅用于**自有设备的软件行为研究与兼容性测试**。不修改服务端、不伪造支付、不绕过计费。所有改动仅作用于本地进程内存与本地缓存展示，不涉及服务端与支付链路。使用者自行承担一切后果。

本项目代码 MIT；Liquid Glass 依赖 Apache-2.0（见 `docs/LICENSE-backdrop-Apache2.0.txt`）。
