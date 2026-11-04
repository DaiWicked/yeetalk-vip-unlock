# YeeTalk VIP Unlocker · LSPosed Module

面向 **YeeTalk 2.58.2**（`com.imback.palpal`）的 LSPosed 模块：在运行期把目标 App 的**本地 VIP 状态与本地门控**翻转为"已开通"。

UI 采用 **Liquid Glass（玻璃拟态）** 风格，底层复用 [Kyant0/Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)（Apache-2.0）。

---

## 状态

| 阶段 | 内容 | 状态 |
|---|---|---|
| P0 | 环境 / 样本解包 / Manifest | ✅ |
| P1 | 关键坐标定位（判定链、存储、权益结构） | ✅ |
| P2 | 模块骨架 + Compose 玻璃 UI | ✅ 代码就位，**未编译验证** |
| P3 | 装机验证矩阵 V1–V6 | ⬜ 待设备 |
| P4 | release 打包 | ⬜ |

> 本仓库**只含源码**。样本 APK、反编译产物、工具链均在 `artifacts/`（已 git 忽略），说明见 [docs/ARTIFACTS.md](docs/ARTIFACTS.md)。

---

## 目录

```
.
├── docs/
│   ├── PROJECT_PLAN.md              项目清单：坐标表、里程碑、权益分层
│   ├── UI_DESIGN.md                 Liquid Glass UI 复用方案与组件映射
│   ├── ARTIFACTS.md                 本地产物清单（不入库）
│   └── LICENSE-backdrop-Apache2.0.txt
├── module/                          LSPosed 模块（:module）
│   └── src/main/
│       ├── assets/xposed_init       Xposed 入口声明
│       ├── res/                     字符串 / 主题 / xposed_scope
│       └── java/com/lcs/yeetalk/
│           ├── HookEntry.kt         IXposedHookLoadPackage 分派
│           ├── MainActivity.kt      Compose 设置页宿主
│           ├── core/                Cfg / LogX / Prefs
│           ├── hook/                H01–H06 各 hook 单元
│           └── ui/                  玻璃组件与设置界面
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradle/libs.versions.toml
└── artifacts/                       ← git 忽略（样本 + 反编译 + 工具）
```

---

## 构建

> 依赖体积较大（Compose + AGP），**gradle 缓存全部落在仓库内的 `.gradle-home/`**，不污染系统盘。首次构建需要联网。

```bash
export GRADLE_USER_HOME="$PWD/.gradle-home"       # Windows: set GRADLE_USER_HOME=%cd%\.gradle-home
./gradlew :module:assembleRelease
```

产物：`module/build/outputs/apk/release/module-release.apk`

**版本基线**（与 Backdrop 上游对齐，见 `gradle/libs.versions.toml`）：
Gradle 9.7.1 · AGP 9.3.2 · Kotlin 2.4.10 · Compose Multiplatform 1.12.0 · Backdrop 2.0.1

若首次构建因版本不可用失败，降级路径见 [docs/UI_DESIGN.md §5](docs/UI_DESIGN.md)。

---

## 安装

1. 设备：Android 10+ / arm64 / Magisk + Zygisk + LSPosed
2. 安装 `module-release.apk`
3. LSPosed 管理器中启用模块，作用域勾选 **YeeTalk (`com.imback.palpal`)**
4. 强杀 YeeTalk 并重启
5. `adb logcat -s XposedBridge | grep YTVIP` 应出现 `========== [YTVIP] 开始注入 ==========` 与各单元 `H01 字段定位：...` `hook UserInfo.K → true (1 处)`

---

## Hook 单元

| 文件 | 目标 | 作用 |
|---|---|---|
| `H01_UserInfo.kt` | `UserInfo.f76016E/f76017F` + `K/H/I/J/L/N` | 字段注入；`K()→true`，`H/I/J/L/N→false`（总闸） |
| `H02_Manager.kt` | `UserInfoManager.C/E/D/F/G` | 统一出口 → `1/true` |
| `H03_Visitor.kt` | `ih.UserVisitorPageState` | 解锁额度放大 |
| `H04_Greet.kt` | `Bb.GreetLimitChargeVipGuide` | 先 dump 签名，白名单命中才翻转 |
| `H05_VipInfo.kt` | `Bb.VipChargeInfo.m()` | 套餐页"已开通" |
| `H06_Dump.kt` | `Bb.VipPrivilegeTableItemInfo` | 只读 dump 权益全表 |

每个单元独立开关，互不依赖；`HookEntry` 带包名守卫与版本告警，版本不符时仍以"找不到就跳过"的防御模式装载，不会中断目标 App。

---

## 免责

仅用于**自有设备的软件行为研究与兼容性测试**。不修改服务端、不伪造支付、不绕过计费。所有改动仅作用于本地进程内存与本地缓存展示。使用者自行承担一切后果。

本项目代码 MIT；Liquid Glass 依赖 Apache-2.0（见 `docs/LICENSE-backdrop-Apache2.0.txt`）。
