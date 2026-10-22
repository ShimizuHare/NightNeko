package com.position.nightneko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.position.nightneko.ui.theme.NightNekoTheme
import java.util.Locale
import java.util.Calendar
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightNekoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NightNekoScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun NightNekoScreen(modifier: Modifier = Modifier) {
    var previewMinute by rememberSaveable { mutableFloatStateOf(90f) }
    val minuteOfDay = previewMinute.roundToInt().coerceIn(0, 1439)
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val actualDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    val dateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault())
    val displayDate = NightTime.displayDate(actualDate.time, actualDate.timeZone)
    fun time(hour: Int) = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("NightNeko", style = MaterialTheme.typography.headlineLarge)
        Text("夜还没结束。", style = MaterialTheme.typography.titleMedium)
        Text("让凌晨的时间接着昨天走：00:00–03:59 显示为 24:00–27:59，04:00 起恢复正常。")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("时间预览", style = MaterialTheme.typography.titleMedium)
                Text(time(NightTime.displayHour(hour)), style = MaterialTheme.typography.displayLarge)
                Text("实际时间 ${time(hour)}")
                Text(dateFormat.format(displayDate))
                Text("实际日期 ${dateFormat.format(actualDate.time)}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = previewMinute,
                    onValueChange = { previewMinute = it },
                    valueRange = 0f..1439f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("拖动查看一天中的显示效果。预览不会改变手机时间。", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("使用方式", style = MaterialTheme.typography.titleLarge)
        Text("1. 在 LSPosed 中启用 NightNeko。\n2. 作用域选择“系统界面”（com.android.systemui）和 Nova Launcher（com.teslacoilsw.launcher）。\n3. 重启手机，让模块生效。")
        Text("设备适配", style = MaterialTheme.typography.titleLarge)
        Text("努比亚 Z60 Ultra · Android 14 · MyOS 14.5\n状态栏、当前数字锁屏样式，以及 Nova 中的时钟 App 桌面小组件。")
        Text("凌晨 00:00–03:59，锁屏与下拉面板日期、星期也延续前一天，04:00 恢复当天。系统实际日期、闹钟、通知时间、应用内时间保持正常。其他桌面启动器、锁屏主题、双时钟与息屏显示暂未适配。")
        Text("此页面仅提供规则预览，不代表模块已在系统界面中生效。", style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun NightNekoPreview() {
    NightNekoTheme {
        NightNekoScreen()
    }
}
