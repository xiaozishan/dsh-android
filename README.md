# DSH 掌上版 (dsh-android)

在 Android 手机上使用 DeepSeek Harness（DSH）的原生应用，Android 16 目标（compileSdk/targetSdk 36）。

## 路线

| 版本 | 方案 | 状态 |
|:---|:---|:---|
| **v1** | Termux 跑 `dsh web` 内核 + 本 App 作为 WebView 壳（引导页 / 令牌连接 / 服务检测） | ✅ 已出包 |
| **v2** | 单 APK 完全体：内嵌 Termux 源 aarch64 Node + bash + ripgrep（payload 管线），前台服务 spawn DSH | 🔧 攻坚中 |

v2 引擎层参考 [aojiepp/dsh-mobile](https://github.com/aojiepp/dsh-mobile)（MIT，见 `reference/`，不入库），应用层（图标 / 主题 / 界面）为本仓库自有实现。

## v1 快速开始

1. 安装 `app-debug.apk`（debug 签名）
2. 安装 [Termux](https://f-droid.org/en/packages/com.termux/)（F-Droid 版）
3. Termux 里运行 `scripts/setup-dsh-termux.sh`，以后启动只需 `dsh-mobile`

## v1 构建

```powershell
# 需要：Android SDK（platform-36 / build-tools 36）、JDK 17+
# 本机参考配置：sdk.dir 见 local.properties（不入库）
gradle assembleDebug        # 或 D:\applications\gradle-8.14\bin\gradle.bat
```

## v2 构建（参考实现管线）

```powershell
# 需要：Python 3、网络（首次下载 Termux .deb）
python payload\extract_termux.py extract     # 解包 node/bash/rg 及依赖 .so
python payload\prepare_dsh_tree.py           # 裁剪 DSH 依赖树（需 npm i -g @deepseek-ai/dsh）
python payload\build_payload.py              # 打包 payload.zip
```

## 图标与主题

- 应用图标取自 DSH Desktop 官方资源（`app-icon.png` / `tray-iconTemplate.png`）
- 自适应图标含 **monochrome 单色图层** → ColorOS / 原生 Android 深色模式与主题图标自动适配
- `values-night` 暗色主题随系统切换

## 许可

应用层代码：MIT。DSH 及相关资源归 DeepSeek 所有，见其官方仓库 [deepseek-ai/deepseek-harness](https://github.com/deepseek-ai/deepseek-harness)。
