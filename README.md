# NightNeko

将系统界面中的凌晨时间延续为前一天的 24–27 时，并在凌晨将日期与星期显示为昨天。基于 LSPosed 的显示层模块。

| 实际时间 | 显示时间 |
| --- | --- |
| 23:59 | 23:59 |
| 00:00 | 24:00 |
| 01:00 | 25:00 |
| 03:59 | 27:59 |
| 04:00 | 04:00 |

## 当前范围（0.3.1）

针对 NX721J / Android 14 / MyOS14.5.6MR6_NX721J_VG，系统界面版本 14.5.001.101.2408221539。
ADB 检查已确认该系统具有以下方法及运行时控件；用户已确认系统时间、系统日期/星期、桌面时钟小时及 0.3.1 小组件日期/星期修正正常生效。

- 状态栏时间：`com.android.systemui.statusbar.policy.Clock.getSmallTime()`，保留分钟、秒数和文本样式。
- 当前锁屏：`com.zte.mifavor.keyguard.settings.LockScreenClockClipTextClock`，仅处理资源 ID 为 `clock_hour_view` 的小时控件。
- 锁屏与通知面板日期：使用 SystemUI 原有本地化日期格式和星期适配器；00:00–03:59 显示前一天，04:00 起显示实际日期。
- 桌面时钟：时钟 App `zte.com.cn.alarmclock` 版本 `14.5.000.000.2406241653` 的 `HorizontalAlarmWidget`，通过 Nova Launcher 中的 `TextClock` 显示；时间资源 `appwidget_text_time_mfvclr` 的映射与状态栏相同，日期/星期资源 `appwidget_text_day_week_mfvclr` 在 00:00–03:59 使用原有格式显示前一天，04:00 恢复当天。
- LSPosed 作用域为 `com.android.systemui` 与 `com.teslacoilsw.launcher`，并核验主进程名；Nova 内其他 TextClock 和其他 AppWidget 不处理。
- 仅向目标显示控件传入日期副本；不修改系统时钟、时区、Calendar、应用取时结果或真实日期。
- 时钟 App 内部界面、闹钟、通知时间戳、其他桌面启动器、双时钟、其他主题和 AOD 不在本版范围内。

## 安装

1. 安装构建出的 APK。
2. 在 LSPosed 中启用 NightNeko，作用域选择“系统界面”/ `com.android.systemui` 和 Nova Launcher / `com.teslacoilsw.launcher`。
3. 重启手机。

应用内滑块仅预览规则，不会修改真实时间，也不用于确认 Hook 是否生效。
不需要给模块授予 Root 或修改系统时间的权限。

## 构建与本地检查

保留项目已有的 Gradle / AGP / SDK 配置。Xposed API 82 从 `https://api.xposed.info/` 解析，使用 `compileOnly`，不会打包框架 API。

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

APK：`app/build/outputs/apk/debug/app-debug.apk`。

本机命令行 Java 使用短名称临时路径时出现过 `Unable to establish loopback connection`。如遇同样问题，可在当前 PowerShell 会话中先执行以下命令再构建；不需要修改系统环境变量：

```powershell
New-Item -ItemType Directory -Path "$PWD\.gradle\tmp" -Force | Out-Null
$env:JAVA_TOOL_OPTIONS += " -Djava.io.tmpdir=$PWD\.gradle\tmp -Djdk.net.unixdomain.tmpdir=$PWD\.gradle\tmp"
```

2026-10-22：0.3.0 Debug APK 构建、单元测试及 Lint 通过；用户确认系统时间、系统日期/星期及桌面小组件小时显示正常。

2026-10-23：0.3.1 修复小组件日期/星期未延续前一天的问题，用户已确认功能正常。Debug APK 构建成功，9 项单元测试通过，Lint 无错误；APK 签名校验通过，沿用原有 Debug 签名。新增日志 `NightNeko: Clock app widget date/weekday view found in Nova Launcher` 用于确认日期控件已匹配。

## 真机测试清单

- 白天与 23:59 显示不变。
- 00:00 → 24:00，01:xx → 25:xx，03:59 → 27:59，04:00 → 04:00。
- 00:00–03:59 锁屏与通知面板日期/星期为昨天；04:00 恢复今天。
- Nova 桌面时钟小组件在 00:00–03:59 与状态栏显示一致（如 `02:38` → `26:38`），04:00 恢复；分钟保持不变。
- Nova 小组件的日期与星期在 23:59 保持当天，00:00–03:59 保持前一天，04:00 恢复实际当天；包括跨月、跨年与星期日/星期一切换。重新添加小组件、横竖屏切换及重新亮屏后也应一致。
- 检查跨月、跨年、闰日和星期变化；同时观察状态栏、下拉面板时钟、当前锁屏小时，分钟不变。
- 锁屏后重新亮屏、解锁、横竖屏切换后持续正常；时区变化后按控件自己的本地小时判断。
- 分离小时/分钟的锁屏中，24–27 无裁切、错位或残影。
- 闹钟、通知时间戳、任一应用内部时间和系统真实日期保持实际值。
- 在 LSPosed 日志中检查 `NightNeko: status clock hooks installed`、`MyOS lock clock hook installed`、`NightNeko: lock-screen and shade DateView hook installed`、`NightNeko: variable shade date hook installed`、`NightNeko: Nova Clock app widget hook installed` 和 `NightNeko: Clock app widget time view found in Nova Launcher`；安装日志只能确认 Hook 注册，不能代替显示验证。

建议等待自然跨午夜观察；不要通过修改系统时间来测试，以免影响其他应用。
本地单元测试覆盖小时映射、04:00 日期切换、跨月/年/闰年/星期边界、夏令时日历日处理、分离小时、秒数、方向标记、部分本地化数字及重复处理。

## 实现约束与恢复

时间每次使用控件原本的 Calendar 读取小时，只替换最终文本。日期按对应格式器的时区创建独立日期副本；正常刷新在 04:00 自行恢复今天的文本。
锁屏 Hook 虽安装在框架 `TextClock.onTimeChanged()`，但仅在 SystemUI 进程生效，并同时核验具体类名与资源 ID。桌面 Hook 仅在 Nova 主进程安装，并核验 RemoteViews 资源所属包及时间控件 ID；不 Hook 时钟 App 内部页面，也不全局 Hook 时间 API、日期格式化 API 或 `TextView.setText`。
缺失方法或异常仅记录一次并尽量保持系统原行为；不同 ROM 版本和主题仍可能不兼容。

如出现异常，在 LSPosed 中停用 NightNeko 后重启。保留设备现有的 Root/LSPosed 恢复方式。
