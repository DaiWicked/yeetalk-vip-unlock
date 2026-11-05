# UserInfo 判定族语义对照表（2.58.2 反编译核实）

> 这份表是"泛化 hook"的安全边界。**单字母方法名不可猜，必须逐个看实现再决定改不改。**
> 踩过的坑：第一版 H07 把 `A() C() D() G()` 全设 true，结果当前用户被当成"官方客服 / 系统号 999 / 官方号 1000"，登录态直接被服务端踢掉。

## UserInfo（`com.imback.commonbase.entity.UserInfo`）

| 方法 | 实现 | 语义 | 处理 |
| --- | --- | --- | --- |
| `A()` | `f76045h ∈ {OFFICIAL_MSG, NOBLE_CUSTOMER_SERVICE, YEETALK_TEAM, ROOM_INTERACTION_NOTICE}` 或 `f76047j.f75905b == 999` | 官方号 / 贵族客服 / 官方团队 / 房间通知账号 | **不动** |
| `C()` | `f76047j.f75905b == 999` | 系统号（level 999） | **不动** |
| `D()` | `TextUtils.equals(f76045h, ROOM_INTERACTION_NOTICE)` | 房间互动通知 | **不动** |
| `E()` | `!p() && f76052o` | 未定性（`p()` = `f76015D == 1` 即 hide 字段） | **不动** |
| `F()` | `f76024M == 1` | 未定性（`f76024M` 为 int 字段） | **不动** |
| `G()` | `TextUtils.equals(f76045h, "1000")` | 官方号（userId 1000） | **不动** |
| `H()` | `f76017F > 0 && !K()` | 曾开通但已过期 | 保持 `false` |
| `I()` | `H() && 已过期时长 < 10 天` | 刚过期（挽留弹窗用） | 保持 `false` |
| `J()` | `H() && 已过期时长 ≥ 10 天` | 过期很久 | 保持 `false` |
| `K()` | `f76016E == 1` | **is_vip，总闸** | **`true`**（H01） |
| `L()` | 距离过期 < 10 天 | 即将到期 | 保持 `false` |
| `N()` | 距离过期 ≥ 10 天 | 长期有效 | 保持 `false` |

> `H/I/J/L/N` 设 true 会让 UI 显示"会员已过期/即将到期"并弹挽留窗，**比原状态更糟**，所以一律 false。

## UserInfoManager（`com.imback.commonbase.utils.UserInfoManager`，Kotlin object）

| 方法 | 实现 | 语义 | 处理 |
| --- | --- | --- | --- |
| `A()` | 转发 `w().A()` | 官方号判定 | **不动** |
| `B()` | 转发 `w().F()` | 未定性 | **不动** |
| `C()` | `w().K() ? 1 : 0` | **is_vip 统一出口（int）** | **`1`**（H02） |
| `D()` | VIP 派生 | 会员态 | `truthy`（H02） |
| `E()` | `C() == 1` | 会员态 | `truthy`（H02） |
| `F()` | VIP 派生 | 会员态 | `truthy`（H02） |
| `G()` | `vip_expire` 距今 < 259200s（3 天） | 即将到期门控 | `truthy`（H02） |
| `H()` | `w() == null \|\| t().length() == 0` | **未登录 / 昵称为空** | **绝不能动** |

## 带 vip / noble 注解的字段（抗混淆锚点）

| 字段 | 类型 | 注解 | 说明 |
| --- | --- | --- | --- |
| `E` | int | `is_vip` | 会员标志，1 = 有效 |
| `F` | int | `vip_expire` | 到期时间（秒级 32 位时间戳，上限 2038-01-19） |
| `L` | int | `noble_ranking` | 贵族榜排名（含 noble 注解） |

**定位字段一律用注解名（`is_vip` / `vip_expire`），不要猜 `f76016E` 这类混淆名** ——
真机 dump 出来字段名就是 `E` / `F`，jadx 的长名只是反混淆别名。

## 结论：有效的覆盖面只剩两层

1. `UserInfo.f76016E == 1`（字段注入 + `K()` 改写）→ H01
2. `UserInfoManager.C()` 返回 1（`D/E/F/G` 一并 truthy）→ H02

这两处是全 App 会员状态的**唯一上游**，已全覆盖。再往上（`VipChargeInfo` 等业务类）类名被 R8
混淆成单字母，且数据来自服务端响应，本地改写没有意义。
