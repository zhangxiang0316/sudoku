# 数独乐园

给家里小朋友练脑用的原生 Android / iOS 数独。

- **三种盘面**：🐣 4×4 入门 → 🐥 6×6 进阶 → 🦉 9×9 标准，默认从 4×4 开始
- **四档难度**：轻松 / 进阶 / 挑战 / 大师，每局唯一解
- 笔记、撤销、提示、暂停、自动存档、通关星级
- 🔔 **音效与震动**：填对轻响、填错轻震、通关小旋律，右上角一键静音
- 💡 **错题说人话**：填错时提示「这一列已经有 3 了哦」，并把冲突格高亮出来
- Android：Kotlin + Jetpack Compose；iOS：Swift + SwiftUI
- 两端均离线运行，无广告、不请求网络权限

## iOS 原生版

用 Xcode 打开 `ios/SudokuKids.xcodeproj`，选择 iPhone 模拟器或已连接的 iPhone，运行 `SudokuKids` scheme。最低系统 iOS 16.0；iPad 可构建运行。

命令行模拟器构建与测试：

```bash
xcodebuild -project ios/SudokuKids.xcodeproj \
  -scheme SudokuKids \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -derivedDataPath ios/build/DerivedData build

xcodebuild -project ios/SudokuKids.xcodeproj \
  -scheme SudokuKids \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -derivedDataPath ios/build/DerivedData test
```

在 Xcode 中选中 `SudokuKids` scheme 后使用 Product → Test（⌘U）运行 XCTest。连接真机运行时，需要在 Signing & Capabilities 里选择自己的 Apple 开发团队。

iOS 使用自己的 UserDefaults 本地存档；Android 与 iOS 的排行榜和存档彼此独立，不会自动同步。

## 运行

需要 JDK 17 和 Android SDK（compileSdk 36）。用 Android Studio 打开会自动生成
`local.properties`；命令行构建则设置 `ANDROID_HOME` 环境变量即可。

```bash
./gradlew installRelease   # 装到设备（正式版，1MB）
./gradlew installDebug     # 调试版（11MB，带调试工具）
./gradlew testDebugUnitTest
```

**下载安装包**：[Releases · 最新版](https://github.com/zhangxiang0316/sudoku/releases/latest)（1.0 MB，Android 8.0+）

本地构建的产物在 `app/build/outputs/apk/release/app-release.apk`。

> release 签名密钥放在 `keystore.properties`（不入库）。缺少该文件时 release 构建仍可完成，只是不签名。

或用 Android Studio 打开本目录直接运行。

详细规划见 [docs/规划与实现说明.md](docs/规划与实现说明.md)。
