# 数独乐园

给家里小朋友练脑用的原生 Android 数独。

- **三种盘面**：🐣 4×4 入门 → 🐥 6×6 进阶 → 🦉 9×9 标准，默认从 4×4 开始
- **四档难度**：轻松 / 进阶 / 挑战 / 大师，每局唯一解
- 笔记、撤销、提示、暂停、自动存档、通关星级
- 🔔 **音效与震动**：填对轻响、填错轻震、通关小旋律，右上角一键静音
- 💡 **错题说人话**：填错时提示「这一列已经有 3 了哦」，并把冲突格高亮出来
- Kotlin + Jetpack Compose，无需联网，无广告

## 运行

需要 JDK 17 和 Android SDK（compileSdk 36）。用 Android Studio 打开会自动生成
`local.properties`；命令行构建则设置 `ANDROID_HOME` 环境变量即可。

```bash
./gradlew installRelease   # 装到设备（正式版，1MB）
./gradlew installDebug     # 调试版（11MB，带调试工具）
./gradlew testDebugUnitTest
```

安装包：`app/build/outputs/apk/release/app-release-signed.apk`（1.0 MB）

或用 Android Studio 打开本目录直接运行。

详细规划见 [docs/规划与实现说明.md](docs/规划与实现说明.md)。
