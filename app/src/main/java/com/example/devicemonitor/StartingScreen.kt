package com.example.devicemonitor

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.view.Choreographer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.Long
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.Unit

@Composable
fun StartingScreen(
    viewModel: RecordViewModel,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fpsMonitor by MonitoringService.fps.collectAsState()
    val batteryPercentageMonitor by MonitoringService.batteryPercentage.collectAsState()
    val batteryTemperatureMonitor by MonitoringService.batteryTemperature.collectAsState()
    val isAppRecording by MonitoringService.isRecording.collectAsState()

    val deviceInfo = DeviceInfo()

    val batteryInfo = rememberBatteryInfo()

    val performanceInfo = rememberPerformanceInfo()

    val context = LocalContext.current
    val storageInfo = remember { fetchStorageInfo(context) }
    val memoryInfo = remember { fetchMemoryInfo(context) }

    var isRecording by remember {mutableStateOf(false)}
    val activeSessionRecords = remember { mutableStateListOf<MetricRecord>() }
    LaunchedEffect(isRecording) {
        while (isRecording) {
            activeSessionRecords.add(
                MetricRecord(
                    timestamp = System.currentTimeMillis(),
                    batteryPercent = batteryInfo.percentage,
                    batteryTemperature = batteryInfo.temperatureCelsius,
                    fps = performanceInfo.fps
                )
            )
            delay(1000L.milliseconds)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column() {
                Text(
                    text = "Device: ${deviceInfo.manufacturerName} ${deviceInfo.modelName}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(4.dp)
                )
                Text(
                    text = "SoC: ${deviceInfo.socName}",
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Android ${deviceInfo.androidVersion} ",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f),

            ) {
                Column() {
                    Text(
                        text = "Battery: ${if (batteryPercentageMonitor >= 0) "${batteryPercentageMonitor}%" else "Unavailable"}",
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = "Temperature: $batteryTemperatureMonitor °C",
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column() {
                    Text(
                        text = "FPS:",
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = "$fpsMonitor",
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f),

                ) {
                Column() {
                    Text(
                        text = "Storage used: ${storageInfo.usedStorage}",
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = "Storage total: ${storageInfo.totalStorage}",
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column() {
                    Text(
                        text = "Ram used: ${memoryInfo.usedRam}",
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = "Ram Total: ${memoryInfo.totalRam}",
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Button(
                onClick = { onToggleRecording() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(isAppRecording) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAppRecording) "Stop Recording" else "Start Recording",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


fun formatTimestamp(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
data class MetricRecord(
    val timestamp: Long,
    val batteryTemperature: Float,
    val batteryPercent: Int,
    val fps: Int,
)
data class ListMetricRecord(
    val timestamp: List<Long>,
    val batteryTemperature: List<Float>,
    val batteryPercent: List<Int>,
    val fps: List<Int>,
)
data class DeviceInfo(
    val manufacturerName: String = Build.MANUFACTURER,
    val modelName: String = Build.MODEL,
    val socName: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Build.SOC_MODEL
    } else {
        Build.HARDWARE
    },
    val androidVersion: String = Build.VERSION.RELEASE,
)
data class BatteryInfo(
    val percentage: Int = 0,
    val temperatureCelsius: Float = 0f
)
data class FPSInfo(
    val fps: Int = 0
)
data class StorageInfo(
    val totalStorage: String = "0 B",
    val usedStorage: String = "0 B",
)
data class MemoryInfo(
    val totalRam: String = "0 B",
    val usedRam: String = "0 B",
)

@Composable
fun rememberBatteryInfo(): BatteryInfo {
    val context = LocalContext.current
    var batteryInfo by remember { mutableStateOf(BatteryInfo()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val percentage = if (level >= 0 && scale > 0) {
                        ((level.toFloat() / scale) * 100).toInt()
                    } else {
                        -1
                    }

                    val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    val temperatureCelsius = rawTemp / 10f

                    batteryInfo = BatteryInfo(
                        percentage = percentage,
                        temperatureCelsius = temperatureCelsius
                    )
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(receiver, filter)

        if (initialIntent != null) {
            val level = initialIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = initialIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale) * 100).toInt()
            } else {
                -1
            }
            val rawTemp = initialIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            batteryInfo = BatteryInfo(
                percentage = percentage,
                temperatureCelsius = rawTemp / 10f
            )
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return batteryInfo
}
@Composable
fun rememberPerformanceInfo(): FPSInfo {
    var fps by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        var frameCount = 0
        var lastSampleTime = System.nanoTime()

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val elapsedNanos = frameTimeNanos - lastSampleTime

                // Update FPS reading every 500 ms for UI stability
                if (elapsedNanos >= 500_000_000L) {
                    fps = ((frameCount * 1_000_000_000.0) / elapsedNanos).roundToInt()
                    frameCount = 0
                    lastSampleTime = frameTimeNanos
                }

                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        Choreographer.getInstance().postFrameCallback(callback)

        onDispose {
            Choreographer.getInstance().removeFrameCallback(callback)
        }
    }
    return FPSInfo(fps = fps)
}
fun fetchStorageInfo(context: Context): StorageInfo {
    val path = Environment.getDataDirectory()
    val stat = StatFs(path.path)
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availableBlocks = stat.availableBlocksLong

    val totalBytes = totalBlocks * blockSize
    val freeBytes = availableBlocks * blockSize
    val usedBytes = totalBytes - freeBytes

    return StorageInfo(
        totalStorage = Formatter.formatFileSize(context, totalBytes),
        usedStorage = Formatter.formatFileSize(context, usedBytes),
    )
}
fun fetchMemoryInfo(context: Context): MemoryInfo {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val totalBytes = memoryInfo.totalMem
    val freeBytes = memoryInfo.availMem
    val usedBytes = totalBytes - freeBytes

    return MemoryInfo(
        totalRam = Formatter.formatFileSize(context, totalBytes),
        usedRam = Formatter.formatFileSize(context, usedBytes),
    )
}