# 构建与安装

安装前先核对 README 的 [适用条件](../README.md#安装前适用条件)：最低 Android 8.0 / API 26，当前只在指定 TCL 85Q10G Android 9 固件验证。先确认可安装普通应用，再依次验证手动运行、无障碍跳转、自启动及专用信号源接口，不把某一阶段成功视为全部兼容。

## 构建

准备 JDK 17、Android SDK Platform 34、Build Tools 34.0.0、Platform Tools。配置 JAVA_HOME、ANDROID_HOME；也可在 `launcher/local.properties` 设置 sdk.dir。本机路径不要提交。

PowerShell：

```powershell
cd launcher
.\gradlew.bat assembleRelease lintRelease testReleaseUnitTest --console=plain
```

Linux/macOS 使用 `bash ./gradlew`。首次构建需要下载 Gradle 和依赖。产物位于 `launcher/app/build/outputs/apk/release/app-release.apk`。

当前 release 使用本机 debug 签名，适合个人侧载。不同电脑或 CI 的密钥通常不同，APK 无法相互覆盖；已有安装请保留原签名密钥。公开发布二进制前自行配置固定 release 签名，禁止把密钥提交仓库。源码不附带 APK 或签名文件。

## 连接与安装

开启电视 ADB/USB 调试，电脑与电视处于可互访网络，在电视确认调试授权。以下 `<TV_IP>` 必须替换成自己的地址：

```powershell
adb connect <TV_IP>:5555
adb devices
adb -s <TV_IP>:5555 install -r launcher/app/build/outputs/apk/release/app-release.apk
adb -s <TV_IP>:5555 shell am start -n com.gathin.tvhome/.MainActivity
```

若固件提示 `install apk has be disabled from pm by system default!`，普通安装方式不可用。本次设备通过 PackageInstaller session 写入并提交成功，辅助源码见 `tools/SessionInstaller.java` 和 [受限安装](TCL-INSTALL.md)。该接口使用 Android 9 隐藏接口，其他版本可能失败，也不能绕过 TCL 对 HOME 应用的签名限制。

## 开启跳转

1. 手动打开“主屏幕”，先确认显示、遥控器、设置和信号源正常。
2. 在电视无障碍设置仅开启 TCL 主页跳转服务。可尝试用 ADB 打开设置页面：`adb -s <TV_IP>:5555 shell am start -a android.settings.ACCESSIBILITY_SETTINGS`。
3. TV 卫士 → 权限管理 → 应用自动启动：实测固件需关闭“自动管理”后，逐项允许“主屏幕”。
4. 在“应用关联启动”中同样允许“主屏幕”。其他应用保持用户原有选择。
5. 按 Home、重启并断电冷启动分别验证。原主页仍可能闪现。

固件路径及后台策略可能不同。不要把本应用设为系统 HOME，也不要禁用原厂主页。旧版上游 HomeKeyService 不适用于此方案，应只启用 TclHomeRedirectService。

## 天气城市

主页左上角的天气区域可以用遥控器选中，按确定进入城市设置。也可以从信号源按钮按左键到达天气区域。输入城市名称并搜索，核对结果中的省份/国家后选择保存；网络失败时保留原选择，可再次搜索。首次使用未选城市时不请求天气。

城市名称查询使用 Open-Meteo / GeoNames；支持中文，未找到时尝试拼音或英文。选择后自动返回主页并刷新，重启后保留设置。在城市页面可以清除选择，停止后续天气刷新。此功能不需要定位权限或 Google 服务框架。

## 第三方应用名单

默认名单：`com.quark.yun.tv`、`com.netease.filmlytv`、`ar.tvplayer.tv`、`org.videolan.vlc`。请另行获取官方应用并确认支持电视 ABI；实测电视需要 32 位 armeabi-v7a 支持。本仓库不重新分发第三方 APK，不提供播放列表。
