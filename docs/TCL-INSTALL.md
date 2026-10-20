# Android 9 受限安装辅助

仅适用于已允许 ADB shell 调试的电视。先尝试标准 `adb install`；这个备用流程在 README 的实测固件上验证了其安装机制，其他版本不保证可用。不会更改系统安装策略，也不能安装被 TCL 签名策略禁止的 HOME 应用。

准备 JDK 17、SDK 34、Build Tools 34.0.0，以下命令在仓库根目录 PowerShell 执行。替换设备地址，确认 adb devices 中设备已授权。`ANDROID_HOME` 指向自己的 SDK。

```powershell
New-Item -ItemType Directory -Force .cache/session | Out-Null
javac -source 8 -target 8 -cp "$env:ANDROID_HOME/platforms/android-34/android.jar" -d .cache/session tools/SessionInstaller.java
# 确认 javac 成功后再继续
& "$env:ANDROID_HOME/build-tools/34.0.0/d8.bat" --lib "$env:ANDROID_HOME/platforms/android-34/android.jar" --output .cache/session/helper.jar .cache/session/SessionInstaller.class
# 确认 d8 成功后再继续
adb -s <TV_IP>:5555 push .cache/session/helper.jar /sdcard/tcl-home-helper.jar
adb -s <TV_IP>:5555 push launcher/app/build/outputs/apk/release/app-release.apk /sdcard/tcl-home.apk
adb -s <TV_IP>:5555 shell 'CLASSPATH=/sdcard/tcl-home-helper.jar app_process /system/bin SessionInstaller /sdcard/tcl-home.apk'
```

辅助程序通过 PackageInstaller 创建并写入安装 session，先输出 `SESSION_ID=数字`，成功写入后输出 `SESSION_WRITTEN=同一数字`。只有两者均出现且没有异常后才执行下列命令，替换 `<SESSION_ID>`，不要照抄占位符：

```powershell
adb -s <TV_IP>:5555 shell pm install-commit <SESSION_ID>
```

只有输出 `Success` 才表示安装完成。失败时保留报错排查，可用 `pm install-abandon <SESSION_ID>` 清理未完成 session。辅助程序异常退出时，如输出了 `SESSION_ID`，同样应 abandon；不要连续重复创建 session。

完成后通过电视文件管理删除 `/sdcard/tcl-home-helper.jar` 和 `/sdcard/tcl-home.apk`。安装不会自动开启跳转，请继续 INSTALL.md 的手动验证及权限设置。
