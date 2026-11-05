# YeeTalk VIP 破解 · LSPosed Hook 框架项目清单

目标样本：`Yeetalk_2.58.2.apk`（171 MB，2026-11 版本号 2.58.2）
工作区：`H:\yeetalk破解`
终态交付：一个可独立安装的 LSPosed 模块，运行期把 YeeTalk 的本地 VIP 状态与本地门控翻转为"已开通"，且不闪退、可开关。

---

## 0. 现状盘点（已核实）

| 项 | 结论 | 证据 |
|---|---|---|
| 加固 | **无壳**。无 jiagu/shella/legu/bangcle，dex 完整 | `unzip -l` 无 stub 特征；7 个 classes.dex 全部可反编译 |
| 包名 | `com.imback.palpal` | `com/imback/palpal/R.java` |
| 架构 | Kotlin + Java 原生为主体；Flutter 是**混合嵌入**（flutterboost） | `com/imback` 下 1521 个 .java；flutter 相关仅 167 个 |
| 语言栈风险 | 中低。VIP 判定全部在 Java/Kotlin 层，不在 Dart（libapp.so） | 判定逻辑已逐行确认（见 P1） |
| 反编译产物 | `artifacts/jadx_out/sources/`，25022 个 .java | 已有 |
| 缺失产物 | `AndroidManifest.xml`、`res/`、`smali/` | 需跑 apktool |
| 工具 | `artifacts/tools/apktool.jar`、`artifacts/tools/jadx/`（jadx CLI 已就绪） | 已有 |

**结论：走 LSPosed hook 路线可行，且不必碰脱壳。**

---

## 1. 关键坐标表（P1 已完成，直接可用）

### 1.1 本地 VIP 状态存储

```
类:  com.imback.commonbase.utils.UserInfoManager   (Kotlin object，单例)
读:  UserInfoManager.w()      → MMKV.defaultMMKV().decodeParcelable(s.USER_INFO, UserInfo.class)   # 行 603-607
写:  MMKV.defaultMMKV().encode(s.USER_INFO, userInfo)                                              # 行 311
清:  MMKV.defaultMMKV().remove(s.USER_INFO)                                                         # 行 373
```

> 用户 VIP 状态以 **Parcelable 存入 MMKV**，不是 SharedPreferences。想纯本地改文件，要解 Parcel 布局（见 1.2 的 writeInt 顺序）；hook 路线不必碰。

### 1.2 用户实体（核心靶子）

```
类:  com.imback.commonbase.entity.UserInfo   (implements Parcelable, Gson 反序列化)
字段（名字被混淆，按类型和注解认）:
  @InterfaceC6403c("is_vip")      public int f76016E     # 行 38    1=VIP
  @InterfaceC6403c("vip_expire")  public int f76017F     # 行 41    秒级时间戳
  @InterfaceC6403c("hide")        public int f76015D     # 行 33    隐身
  @InterfaceC6403c("noble_ranking") public int f76023L   # 行 74    贵族榜
  @InterfaceC6403c("noble_level"/"noble") 贵族等级同族字段
Parcel 读写顺序（改文件需要）: 行 393-394 readInt / 行 618-619 writeInt，紧邻 is_vip 与 vip_expire 两点
```

### 1.3 本地 VIP 判定函数（**第一 hook 优先级**）

```
com.imback.commonbase.entity.UserInfo
  K()  行 314   return (f76016E == 1)                        ← 是否 VIP（总闸）
  H()  行 291   f76017F > 0 && !K()                          ← 曾经是 VIP / 已过期
  I()  行 298   H() && 过期不足 10 天                         ← 过期未满 10 天
  J()  行 306   H() && 过期超 10 天
  L()  行 321   距过期 < 10 天                                ← 即将过期提醒
  N()  行 329   距过期 >= 10 天
  行 433/437     剩余/已过天数计算

com.imback.commonbase.utils.UserInfoManager
  C()  行 ~148  w().K() ? 1 : 0                              ← VIP 统一出口（int）
  E()  行 ~166  C() == 1
  D()  行 ~154  !E() && w().f76017F > 0
  F()  行 ~176  !E() && w().f76017F > 0  (else true)
  G()  行 ~195  vip_expire 距现在 < 259200 秒（3 天）          ← 即将到期弹窗门控
```

> `K()` 与 `C()` 是全 App 的收敛点。先翻这两个，80% 的"VIP 显示"立刻生效。

### 1.4 VIP 业务数据结构（用于枚举权益、以及改页面态）

```
Bb/VipChargeInfo.java          行 159：isVip==1 && vipExpireTime >= N0.r()  → m() 本地联合判定
                               字段：setData / tips / isVip / vipExpireTime / discountPropInfo
Bb/VipPrivilegeInfo.java       权益列表容器（List<PrivilegeItem>）
Bb/VipPrivilegeTableItemInfo.java  权益表格项：vipTableValue / code / tableValue / name / tableType / icon
                                    ← code 就是权益 key，运行期 dump 一遍即可拿到完整权益清单
Bb/VipProductInfo / VipProductSetInfo / VipDiscountPropInfo / VipProductDiscountPropInfo   套餐与折扣
Bb/GreetLimitChargeVipGuide.java   打招呼次数用尽 → 引导开 VIP
com/imback/user/visitor/UserVisitorsActivity.java 行 442-453   访客页：isVip / visitCount / freeUnlockSize / maxAvailableFreeUnlockIndex
ih/UserVisitorPageState.java   访客页状态（上述四值 + title），行 16-28
com/imback/user/vip/VipAuthorityInfoFragment.java   权益说明页
com/imback/user/vip/BuyVipActivity.java             购买页
com/imback/pay/vip/VipChargeFragment.java           充值页
```

> `VipPrivilegeTableItemInfo.code` 是权益枚举金矿：README 里所有"VIP 特权"文案都从服务端配表下发，运行时把收到的那份 dump 出来，就等于拿到官方权益全表，不用猜。

---

## 2. 阶段与里程碑

### P0 环境补齐（约 20 分钟，一次到位）
1. `java -jar artifacts/tools/apktool.jar d -f -s -o artifacts/apktool_out artifacts/Yeetalk_2.58.2.apk` → 拿 `AndroidManifest.xml`（包名/版本/minSdk/application 类）、`res/values/strings.xml`
2. 从 Manifest 抄下 `application`、主 Activity、`minSdkVersion`、签名方案（v2/v3）、`android:debuggable`
3. 设备侧：真机（arm64，Android 10+）+ Magisk + Zygisk + LSPosed；或 Android Studio AVD（x86_64，需带 Google APIs 的镜像）
4. 校验：装原版 APK，登录，进 VIP 页确认"未开通"基线

**验收**：能 `adb shell dumpsys package com.imback.palpal | grep versionName` 出 2.58.2

### P1 定位（**已完成，见第 1 节**）
不再重复投入。除非 P3 验证失败，才回头补 `VipAuthorityInfoFragment` 的权益字段。

### P2 框架骨架（本项目主体）

```
YeetalkVipHook/
├── build.gradle.kts                 compileSdk 34 / minSdk 26；依赖 xposed api 82（compileOnly）
├── src/main/AndroidManifest.xml     <meta-data name="xposedmodule" .../> + xposeddescription + xposedminversion 93
├── src/main/assets/xposed_init      一行：com.mono.systools.HookEntry
├── src/main/java/com/lcs/yeetalk/
│   ├── HookEntry.java               implements IXposedHookLoadPackage，handleLoadPackage 分派
│   ├── Cfg.java                     常量集中：类名/方法名/开关，全部字符串常量便于改
│   ├── Log.java                     XposedBridge.log 封装 + 环形缓冲（防刷爆 logcat）
│   ├── hook/
│   │   ├── H01_UserInfoField.java   反射写 f76016E=1、f76017F=T+10年
│   │   ├── H02_UserInfoJudge.java   hook K/H/I/J/L/N → 强制 VIP 语义
│   │   ├── H03_UserInfoManager.java hook C/E/D/F/G → 1/true/...
│   │   ├── H04_VisitorQuota.java    hook ih.UserVisitorPageState 的 getter：freeUnlockSize 放大、isVip=true
│   │   ├── H05_GreetLimit.java      hook GreetLimitChargeVipGuide 的判定与次数
│   │   ├── H06_VipInfo.java         hook Bb.VipChargeInfo.m() → true，isVip=1
│   │   └── H07_Dump.java            一次性 dump VipPrivilegeTableItemInfo 全量 → 落 sdcard
│   └── ui/                          （可选）LSPosed 模块内开关界面
└── proguard / 无混淆需求（模块自身不打包进 App）
```

**每层职责（不混写，出问题好定位）**

| 层 | 类 | 手段 | 影响面 |
|---|---|---|---|
| L0 | `Cfg` | 纯常量 | 改一处即可换靶 |
| L1 | `H01` | 反射改字段 | 让 `is_vip/vip_expire` 从源头就是 VIP |
| L2 | `H02 H03` | hook 方法返回值 | 覆盖所有读判定的调用方（页头徽标、弹窗、文案） |
| L3 | `H04 H05 H06` | hook 页面态/引导 | 访客额度、打招呼、折扣展示 |
| L4 | `H07` | 只读 dump | 产出权益全表，喂回 L3 补漏 |
| L5 | `HookEntry` | 版本守卫 + try/catch | 版本不符直接 return，避免注入即崩 |

**骨架核心（可直接落盘编译）**

```java
public class HookEntry implements IXposedHookLoadPackage {
    private static final String PKG = "com.imback.palpal";
    private static final String VERSION = "2.58.2";

    @Override public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!PKG.equals(lpparam.packageName)) return;
        try {
            hookUserInfo(lpparam.classLoader);
            hookManager(lpparam.classLoader);
            Log.i("hook installed on " + VERSION);
        } catch (Throwable t) {
            Log.e("install failed", t);   // 失败也只记日志，绝不抛出
        }
    }

    /** L1+L2：UserInfo 字段与判定 */
    private void hookUserInfo(ClassLoader cl) throws Throwable {
        Class<?> ui = XposedHelpers.findClass("com.imback.commonbase.entity.UserInfo", cl);

        // 字段：is_vip -> 1，vip_expire -> 十年后（秒）
        XposedBridge.hookAllConstructors(ui, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Object self = p.thisObject;
                XposedHelpers.setIntField(self, "f76016E", 1);
                XposedHelpers.setIntField(self, "f76017F",
                        (int) (System.currentTimeMillis() / 1000L) + 315360000);
            }
        });

        // 判定：K 必须为 true（有效会员）
        // H/I/J/L/N 必须为 false —— 它们代表"曾经是 VIP 但已过期 / 即将到期"。
        // 若 K=true 同时 H=true，两条互斥分支会同时命中，UI 显示"会员已过期"
        // 并弹挽留窗，比原状态更糟。这个反直觉点是 2.58.2 实机结论。
        hookBoolean(ui, "K", true);
        for (String m : new String[]{"H", "I", "J", "L", "N"}) hookBoolean(ui, m, false);
    }

    private void hookBoolean(Class<?> c, String name, boolean value) {
        XposedBridge.hookAllMethods(c, name, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult(value); }
        });
    }

    /** L2：UserInfoManager 统一出口 */
    private void hookManager(ClassLoader cl) throws Throwable {
        Class<?> um = XposedHelpers.findClass(
                "com.imback.commonbase.utils.UserInfoManager", cl);
        XposedHelpers.findAndHookMethod(um, "C", new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam p) { return 1; }
        });
        for (String m : new String[]{"E", "D", "F", "G"}) {
            XposedHelpers.findAndHookMethod(um, m, new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(MethodHookParam p) { return true; }
            });
        }
    }
}
```

> 命名坑：`UserInfoManager` 是 **Kotlin object**，`C()` 等方法虽写成实例方法，实际在 `INSTANCE` 上；`XposedHelpers.findAndHookMethod` 对 object 的单例方法可直接按实例方法 hook（Xposed 会绑定唯一实例），无需特判。若报 `NoSuchMethodError`，改用 `hookAllMethods(um, "C", ...)`。

### P3 验证矩阵（每层单独回归，不许一次全开会）

| 用例 | 期望 | 观察点 |
|---|---|---|
| V1 冷启进"我的" | 头像/VIP 徽标点亮，套餐页显示"已开通" | `H01 H02 H03` |
| V2 进 VIP 权益页 | 权益表全亮，倒计时显示剩余天数 | `H06` + `H07` dump |
| V3 访客记录页 | 解锁额度变大、不再提示开会员 | `H04` |
| V4 打招呼到上限 | 不再弹 `GreetLimitChargeVipGuide` | `H05` |
| V5 杀进程重进 | 不崩、不吃到"登录态异常"提示 | `L5` 版本守卫 |
| V6 发 VIP 专属礼物 | **预期失败**（服务端拒），记录返回码 | 判定本地极限边界 |

### P4 打包交付
1. `gradle :app:assembleRelease` → `YeetalkVipHook-1.0.apk`
2. 装机 → LSPosed 勾选 `com.imback.palpal` → 强杀重启
3. `adb logcat -s XposedBridge` 确认 `hook installed on 2.58.2`
4. 保留回滚：模块内加总开关，关掉即回原样

---

## 3. 能破解哪些本地 VIP 权益（分层结论）

### A 层 · 纯本地渲染，改了就亮（确定性最高）
1. **VIP 身份徽标 / 头像挂件**：凡是读 `UserInfo.K()` 或 `UserInfoManager.C()` 的 UI 分支
2. **个人主页 VIP 皮肤与主页特权标识**
3. **VIP 权益页全表点亮**（`VipPrivilegeTableItemInfo` 逐条比对本地 `is_vip`）
4. **"剩余 X 天"倒计时与到期提醒**：篡改 `vip_expire` 后 `L()/N()/G()` 全变真
5. **购买页/充值页的"已是会员"态**（`BuyVipActivity`、`VipChargeFragment`、`VipChargeInfo.m()`）
6. **即将过期挽留弹窗**（`UserInfoManager.G()` 的 3 天阈值门控）

### B 层 · 本地门控，看额度是否随 `is_vip` 在本地算（大概率可破，需 V3/V4 实测）
7. **"谁看过我"访客解锁额度**：`UserVisitorsActivity` 的 `freeUnlockSize` / `maxAvailableFreeUnlockIndex`，本地放大即多看若干条；全量列表仍走接口
8. **打招呼/搭讪次数上限**：`GreetLimitChargeVipGuide` 拦截面，hook 后可继续发
9. **筛选与搜索的高级条件**（性别/语言/国家/在线）——若开关只读本地 VIP，则解锁
10. **隐身访问 / 隐藏在线**（`UserInfo.f76015D` hide 字段）
11. **礼物与套餐的 VIP 折扣展示价**（`VipDiscountPropInfo`，仅影响展示，实际扣费仍按服务端）
12. **贵族 `noble_level` 相邻体系的本地展示**（同一套 hook 手法，换字段即可）

### C 层 · 服务端裁决，本地翻不动（把它当成边界，别死磕）
13. VIP 专属礼物的**实际发送与扣费**
14. 访客记录的全量拉取、真实资料下载/导出
15. 付费聊天的条数与内容解锁
16. 房间/群的人数上限、建房权限等由服务端下发的配额
17. 任何带服务端签名的鉴权结果

> 一句话：**A 层是必成，B 层是主战场，C 层是天花板。** 项目按 A→B→C 推进，C 层只做 V6 边界记录，不投入返工。

---

## 4. 执行纪律（事不过三）

- 同一靶点最多试 **3 种**手段（字段 hock → 方法 hook → 页面态 hook）；第 3 次不成，标记 `BLOCKED` 写进 `NOTES.md`，跳到下一条。
- 单次调试不超过 **15 分钟**：改一轮 → 装机 → 看 logcat → 不成就换点。
- 一次性全开 hook 是最贵的错误：**一层一开**，出问题能定位到单文件。
- 版本守卫永远在最前面：`VERSION` 不匹配直接 return，避免注入即崩把设备搞成砖。
- 产出物落盘到 `H:\yeetalk破解\`：`PROJECT_PLAN.md`（本文）、`NOTES.md`（失败记录与边界）、`dump/privileges.json`（权益全表）。

---

## 5. 下一步（P0 待办，等你一句话）
1. 跑 apktool 出 Manifest 与 res
2. 建 LSPosed 模块工程骨架（P2 那套目录）
3. 先只上 `H01 + H02 + H03`，V1 通过再往下
