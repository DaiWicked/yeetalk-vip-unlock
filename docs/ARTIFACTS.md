# 本地产物清单（不入库）

`artifacts/` 下全部内容已在 `.gitignore` 中排除。这些是**分析素材与工具链**，体积大、可重新生成，不应进入 Git 历史。

| 路径 | 体积 | 内容 | 如何重新生成 |
|---|---|---|---|
| `artifacts/Yeetalk_2.58.2.apk` | 171 MB | 原始样本（versionCode 1955，包名 `com.imback.palpal`） | 从官方渠道获取对应版本 |
| `artifacts/jadx_out/` | 207 MB | jadx 反编译 Java 源码，25,022 个 `.java` | `java -jar artifacts/tools/jadx/lib/jadx-*.jar -d artifacts/jadx_out artifacts/Yeetalk_2.58.2.apk` |
| `artifacts/apktool_out/` | ~250 MB | apktool 解包：`AndroidManifest.xml`、`res/`、`lib/`、`classes*.dex` | `java -jar artifacts/tools/apktool.jar d -f -s artifacts/Yeetalk_2.58.2.apk -o artifacts/apktool_out` |
| `artifacts/tools/apktool.jar` | 23 MB | apktool | 官方 release |
| `artifacts/tools/jadx/` | — | jadx CLI（解压后） | 官方 release |
| `artifacts/tools/jadx.zip` | 105 MB | jadx 原始压缩包 | 官方 release |

## 关键事实（供后续核对，无需打开 artifact）

- 无加固壳：7 个 dex 全部可反编译，无 jiagu/shella/legu/bangcle 特征 → **无需脱壳**
- 主体 Kotlin/Java（`com/imback` 下 1521 个 java）；Flutter 只是混合嵌入（`flutterboost`，167 个 java，`libflutter.so` + `libapp.so`）
- VIP 判定全在 Java/Kotlin 层，Dart 侧不参与 → hook 无需碰 Flutter

## 注意

- 仓库克隆后 `artifacts/` 为空，**不影响编译模块**（模块只依赖 Maven 库）。
- 想复现逆向分析，先把 APK 放回 `artifacts/`，再按上表命令生成。
- 迁移机器时直接拷贝 `artifacts/` 目录即可，不要 `git clone`。
