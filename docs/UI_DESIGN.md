# UI 设计：Liquid Glass 复用方案

## 1. 来源与许可

上游：[Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（`backdrop` 模块）
许可：**Apache-2.0** — 允许商用、修改、再分发，需保留版权与许可声明（全文见 `docs/LICENSE-backdrop-Apache2.0.txt`）。
坐标：`io.github.kyant0:backdrop:2.0.1`

上游只提供**底层绘制能力**（折射、模糊、高光、内阴影），不含成品组件 —— 组件需自己写。这也是我们没有直接抄 `LiquidButton/LiquidToggle` 的原因（它们位于上游的 catalog demo 模块，依赖 `DampedDragAnimation` 等内部实现）。

## 2. 直接使用的公开 API

```kotlin
// 1) 创建玻璃源
val backdrop: LayerBackdrop = rememberLayerBackdrop()   // com.kyant.backdrop.backdrops

// 2) 把背景内容挂进玻璃层（决定折射内容）
Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) { AuroraBackground() }

// 3) 前景玻璃
Modifier.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedCornerShape(26.dp) },
    effects = { blur(14f.dp.toPx()) },          // 可用 vibrancy() / lens() / opacity()
    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.10f)) }
)
```

`drawBackdrop` 完整签名（`DrawBackdropModifier.kt:80`）还支持 `highlight`、`shadow`、`innerShadow`、`layerBlock`（交互形变）、`onDrawBehind`、`onDrawFront`。

## 3. 组件映射（`module/src/main/java/com/lcs/yeetalk/ui/`）

| 我们的组件 | 文件 | 对应的上游参考 | 用途 |
|---|---|---|---|
| `rememberGlassBackdrop()` | `ui/Glass.kt` | `rememberLayerBackdrop` | 页面级玻璃源 |
| `GlassCard` | `ui/Glass.kt` | `LiquidButton` 的 drawBackdrop 段 | 分组容器、卡片 |
| `GlassToggle` | `ui/Glass.kt` | `LiquidToggle` | 6 个 hook 开关 + 总开关 |
| `GlassButton` | `ui/Glass.kt` | `LiquidButton` | 胶囊按钮（+/−、打开 App） |
| `GlassTextField` | `ui/GlassTextField.kt` | —（自绘 BasicTextField） | H04 手动白名单输入 |
| `SectionTitle` / `GlassRow` / `GlassText` | `ui/Glass.kt` | — | 排版原子件 |
| `AuroraBackground` | `MainActivity.kt` | `HomeContent` 的渐变球背景 | 玻璃的折射内容 |

**故意没做的事**：
- 不用 `lens()`：折射半径需要和设备尺寸、内容对比度一起调，先保证能跑
- 不用 `kyant-shapes` 的 `Capsule()`：统一用 `RoundedCornerShape`，少一层依赖耦合
- 不做 `DampedDragAnimation` 那种拖拽弹性：开关只需要 200ms 的 fraction 动画

## 4. 界面结构

```
┌──────────────────────────────┐
│ YeeTalk VIP            (30sp)│
│ 本地权益解锁 · LSPosed 模块   │
│ ┌──────────────────────────┐ │
│ │ 总开关              [ ●] │ │  ← GlassCard + GlassToggle
│ └──────────────────────────┘ │
│  HOOK 单元                    │  ← SectionTitle
│ ┌──────────────────────────┐ │
│ │ VIP 状态            [ ●] │ │
│ │ is_vip / vip_expire …    │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ VIP 统一出口        [ ●] │ │
│ └──────────────────────────┘ │
│  … ×6                        │
│  参数                         │
│ ┌──────────────────────────┐ │
│ │ 访客解锁额度  [−] 999 [+]│ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ H04 手动白名单            │ │
│ │ [a, isLimitReached, c   ]│ │  ← GlassTextField
│ └──────────────────────────┘ │
│  状态                         │
│ ┌──────────────────────────┐ │
│ │ 目标应用  com.imback.palpal│ │
│ │ 注入日志  adb logcat …    │ │
│ │ [   打开 YeeTalk    ]     │ │  ← GlassButton
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

## 5. 版本基线与降级路径

当前 `gradle/libs.versions.toml` 与上游完全对齐：

```
Gradle 9.7.1 · AGP 9.3.2 · Kotlin 2.4.10 · Compose MP 1.12.0 · backdrop 2.0.1
```

**为什么必须对齐**：backdrop 是 `implementation` 依赖 Compose 1.12 编译的，运行时会把对应 Compose runtime 带进来。若我们编译时用更老的 Compose（如 1.7.x），会出现 `NoSuchMethodError`。

首次构建失败时的降级顺序（逐级尝试，每级只试一次）：

1. **换构建工具版本**：把 `agp` / `kotlin` / `compose` 三个版本一起下调到本机 Android Studio 支持的最新稳定版；`backdrop` 保持 `2.0.1`。
2. **不引 Maven，改源码内嵌**：把上游 `backdrop/src` 的 `commonMain` + `androidMain` 拷进 `module/src/main/java/com/kyant/backdrop/`，删掉 `expect/actual` 与 `skikoMain`，作为普通 Kotlin 源码编译（38 个文件，182 KB，Apache-2.0 允许，保留 `docs/LICENSE-backdrop-Apache2.0.txt`）。
3. **降级为纯色 UI**：把 `Glass.kt` 里 `drawBackdrop` 换成 `Modifier.background(color, shape)`，功能完全不受影响，只是没有玻璃质感。

## 6. 视觉参数（可调）

| 参数 | 位置 | 当前值 | 说明 |
|---|---|---|---|
| 卡片着色 | `GlassCard(tintAlpha)` | 0.10 | 越大越"奶白" |
| 总开关卡片 | `tintAlpha` | 0.16 | 强调 |
| 模糊半径 | `GlassCard` 内 `blur()` | 14 dp | 越大越柔 |
| 开关轨道 | `GlassToggle` | 52×32 dp，滑块 26 dp | iOS 尺寸 |
| 强调色 | `accent` | `#34C759` | 与 iOS 开关一致 |
| 背景光斑 | `AuroraBackground` | 粉/蓝/紫 三团，alpha 0.31–0.44 | 决定玻璃通透感 |
