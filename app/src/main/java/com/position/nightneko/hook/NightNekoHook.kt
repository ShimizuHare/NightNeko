package com.position.nightneko.hook

import android.text.SpannableStringBuilder
import android.icu.text.DateFormat
import android.widget.TextClock
import android.widget.TextView
import com.position.nightneko.NightTime
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

class NightNekoHook : IXposedHookLoadPackage {
    private val reported = ConcurrentHashMap.newKeySet<String>()

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (param.packageName == NOVA_LAUNCHER && param.processName == NOVA_LAUNCHER) {
            installLauncherWidgetClockHook()
            return
        }
        if (param.packageName != SYSTEM_UI || param.processName != SYSTEM_UI) return

        installDateHooks(param.classLoader)

        attempt("status-clock-install") {
            val clock = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", param.classLoader)
            XposedHelpers.findAndHookMethod(clock, "getSmallTime", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) = attempt("status-clock-update") {
                    val original = param.result as? CharSequence ?: return@attempt
                    val calendar = XposedHelpers.getObjectField(param.thisObject, "mCalendar") as? Calendar
                        ?: return@attempt
                    param.result = rewrite(original, calendar.get(Calendar.HOUR_OF_DAY))
                }
            })
            XposedHelpers.findAndHookMethod(clock, "updateClock", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) = attempt("status-description-update") {
                    val view = param.thisObject as? TextView ?: return@attempt
                    val calendar = XposedHelpers.getObjectField(view, "mCalendar") as? Calendar ?: return@attempt
                    rewriteDescription(view, calendar.get(Calendar.HOUR_OF_DAY))
                }
            })
            XposedBridge.log("NightNeko: status clock hooks installed")
        }

        attempt("lock-clock-install") {
            // Framework method, but installed only in SystemUI and filtered by class AND resource ID.
            XposedHelpers.findAndHookMethod(TextClock::class.java, "onTimeChanged", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) = attempt("lock-clock-update") {
                    val view = param.thisObject as? TextClock ?: return@attempt
                    if (view.javaClass.name != MYOS_HOUR_CLOCK) return@attempt
                    if (view.id == android.view.View.NO_ID) return@attempt
                    if (view.resources.getResourceEntryName(view.id) != "clock_hour_view") return@attempt
                    val calendar = XposedHelpers.getObjectField(view, "mTime") as? Calendar ?: return@attempt
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val text = view.text ?: return@attempt
                    val rewritten = rewrite(text, hour)
                    if (rewritten !== text) view.text = rewritten
                    rewriteDescription(view, hour)
                    if (reported.add("lock-view-found")) {
                        XposedBridge.log("NightNeko: MyOS lock-screen hour view found")
                    }
                }
            })
            XposedBridge.log("NightNeko: MyOS lock clock hook installed")
        }
    }

    private fun installLauncherWidgetClockHook() {
        attempt("launcher-widget-clock-install") {
            XposedHelpers.findAndHookMethod(TextClock::class.java, "onTimeChanged", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) = attempt("launcher-widget-clock-update") {
                    val view = param.thisObject as? TextClock ?: return@attempt
                    if (view.id == android.view.View.NO_ID) return@attempt
                    val resources = view.resources
                    if (resources.getResourcePackageName(view.id) != CLOCK_APP) return@attempt
                    val entry = resources.getResourceEntryName(view.id)
                    if (entry != CLOCK_WIDGET_TIME_VIEW && entry != CLOCK_WIDGET_DATE_VIEW) return@attempt
                    val calendar = XposedHelpers.getObjectField(view, "mTime") as? Calendar ?: return@attempt
                    if (entry == CLOCK_WIDGET_DATE_VIEW) {
                        rewriteWidgetDate(view, calendar)
                        if (reported.add("clock-widget-date-view-found")) {
                            XposedBridge.log("NightNeko: Clock app widget date/weekday view found in Nova Launcher")
                        }
                        return@attempt
                    }
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val text = view.text ?: return@attempt
                    val rewritten = rewrite(text, hour)
                    if (rewritten !== text) view.text = rewritten
                    rewriteDescription(view, hour)
                    if (reported.add("clock-widget-view-found")) {
                        XposedBridge.log("NightNeko: Clock app widget time view found in Nova Launcher")
                    }
                }
            })
            XposedBridge.log("NightNeko: Nova Clock app widget hook installed")
        }
    }

    private fun rewriteWidgetDate(view: TextClock, actual: Calendar) {
        // onTimeChanged has already refreshed today's text; leave it intact from 04:00 onward.
        if (actual.get(Calendar.HOUR_OF_DAY) >= 4) return
        val format = XposedHelpers.getObjectField(view, "mFormat") as? CharSequence ?: return
        if (format.isEmpty()) return
        val display = (actual.clone() as Calendar).apply {
            time = NightTime.displayDate(actual.time, actual.timeZone)
        }
        // Reuse the active RemoteViews pattern, including localized date order and weekday style.
        val text = android.text.format.DateFormat.format(format, display)
        if (view.text.toString() != text.toString()) view.text = text
        attempt("launcher-widget-date-description") {
            val descriptionFormat = XposedHelpers.getObjectField(view, "mDescFormat") as? CharSequence
            if (!descriptionFormat.isNullOrEmpty()) {
                view.contentDescription = android.text.format.DateFormat.format(descriptionFormat, display)
            }
        }
    }

    private fun installDateHooks(loader: ClassLoader) {
        attempt("date-view-install") {
            val dateView = XposedHelpers.findClass("com.android.systemui.statusbar.policy.DateView", loader)
            XposedHelpers.findAndHookMethod(dateView, "updateClock", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) = attempt("date-view-update") {
                    if (param.hasThrowable()) return@attempt
                    val view = param.thisObject as? TextView ?: return@attempt
                    val actual = XposedHelpers.getObjectField(view, "mCurrentTime") as? Date ?: return@attempt
                    val format = XposedHelpers.getObjectField(view, "mDateFormat") as? DateFormat ?: return@attempt
                    val display = NightTime.displayDate(actual, TimeZone.getTimeZone(format.timeZone.id))
                    // Reuse MyOS's own date adapter, including its localized weekday/date formatting.
                    val adapter = XposedHelpers.getObjectField(view, "mAdapt") ?: return@attempt
                    val text = XposedHelpers.callMethod(adapter, "getCurrentText", display, format.format(display)) as? String
                        ?: return@attempt
                    if (view.text.toString() != text) view.text = text
                    XposedHelpers.setObjectField(view, "mLastText", text)
                    view.contentDescription = text
                }
            })
            XposedBridge.log("NightNeko: lock-screen and shade DateView hook installed")
        }
        attempt("variable-date-install") {
            val helper = XposedHelpers.findClass("com.android.systemui.statusbar.policy.VariableDateViewControllerKt", loader)
            XposedHelpers.findAndHookMethod(helper, "getTextForFormat", Date::class.java, DateFormat::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) = attempt("variable-date-update") {
                        val actual = param.args[0] as? Date ?: return@attempt
                        val format = param.args[1] as? DateFormat ?: return@attempt
                        // Only this SystemUI helper receives a display copy; the controller keeps real time.
                        param.args[0] = NightTime.displayDate(actual, TimeZone.getTimeZone(format.timeZone.id))
                    }
                }
            )
            XposedBridge.log("NightNeko: variable shade date hook installed")
        }
    }

    private fun rewriteDescription(view: TextView, hour: Int) {
        val original = view.contentDescription ?: return
        val rewritten = rewrite(original, hour)
        if (rewritten !== original) view.contentDescription = rewritten
    }

    private fun rewrite(original: CharSequence, hour: Int): CharSequence {
        val replacement = NightTime.replacement(original, hour) ?: return original
        return SpannableStringBuilder(original).replace(replacement.start, replacement.end, replacement.text)
    }

    private inline fun attempt(label: String, action: () -> Unit) {
        try {
            action()
        } catch (error: Throwable) {
            if (reported.add(label)) XposedBridge.log("NightNeko: $label failed; keeping system behaviour: $error")
        }
    }

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
        const val NOVA_LAUNCHER = "com.teslacoilsw.launcher"
        const val CLOCK_APP = "zte.com.cn.alarmclock"
        const val CLOCK_WIDGET_TIME_VIEW = "appwidget_text_time_mfvclr"
        const val CLOCK_WIDGET_DATE_VIEW = "appwidget_text_day_week_mfvclr"
        const val MYOS_HOUR_CLOCK = "com.zte.mifavor.keyguard.settings.LockScreenClockClipTextClock"
    }
}
