# 拾念 Android

这是根据仓库根目录网页原型实现的原生 Android 首版。

## 环境

- Android Studio Rabbit 1 / 2026.2.1
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.3.21
- Jetpack Compose BOM 2026.03.00
- `compileSdk` Android 16 QPR2 / API 36.1
- `minSdk` Android 16 / API 36
- JDK 21

## 已实现

- 生物识别入口与恢复口令演示入口
- 三步资料库创建流程及系统目录选择器
- 时间线、类型筛选、本地搜索和详情页
- 首次启动为空时间线，旧版升级时会自动移除原型内置记录
- 真实文字记录、系统相册选图、调用相机拍照和麦克风录音
- 图片预览、录音播放以及记录删除
- Android Keystore + AES-GCM 加密正文、图片和录音文件
- 数据与安全页面
- 加密备份（含媒体文件）、PDF、JSON 三种本地导出

## 构建

在 Android Studio 中打开本目录，或执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。
