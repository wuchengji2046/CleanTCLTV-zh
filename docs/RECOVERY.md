# 恢复原主页

先在电视无障碍设置关闭 TCL 主页跳转服务，再按 Home 即可停留原主页；可同时关闭 TV 卫士中本应用的自启动/关联启动。

若循环跳转导致无法操作，用已授权 ADB 禁用新桌面后打开原主页。以下只禁用本应用，不禁用原厂主页：

```powershell
adb -s <TV_IP>:5555 shell pm disable-user --user 0 com.gathin.tvhome
adb -s <TV_IP>:5555 shell am start -n com.tcl.cyberui/.MainActivity
```

恢复新桌面：`adb -s <TV_IP>:5555 shell pm enable com.gathin.tvhome`，手动打开并重新检查无障碍和自启动权限。

不再需要时可通过系统应用管理卸载，或执行 `adb -s <TV_IP>:5555 uninstall com.gathin.tvhome`，这会清除本应用数据。

本方案保留原厂主页、TV、设置、TV 卫士和硬件/媒体服务。部分娱乐购物应用曾在实测电视按用户需求停用，但这不是桌面运行的前提。本仓库不提供通用一键精简脚本，不应把某台电视的包名清单套用到其他型号。
