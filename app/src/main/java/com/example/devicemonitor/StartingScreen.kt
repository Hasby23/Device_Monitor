package com.example.devicemonitor

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
data class StorageInfo(
    val totalStorage: String = "0 B",
    val usedStorage: String = "0 B",
)
data class MemoryInfo(
    val totalRam: String = "0 B",
    val usedRam: String = "0 B",
)

@Composable
fun StartingScreen(
    onToggleOverlay: () -> Unit,
    onToggleRecording: () -> Unit,
    requestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fpsMonitor by MonitoringService.fps.collectAsState()
    val batteryPercentageMonitor by MonitoringService.batteryPercentage.collectAsState()
    val batteryTemperatureMonitor by MonitoringService.batteryTemperature.collectAsState()
    val isAppRecording by MonitoringService.isRecording.collectAsState()
    val isAppOverlaying by MonitoringService.isOverlaying.collectAsState()

    val isBinderAlive by MonitoringService.isBinderAlive.collectAsState()
    val hasPermission by MonitoringService.hasPermission.collectAsState()
    val targetQuery by MonitoringService.targetQuery.collectAsState()

    val deviceInfo = DeviceInfo()
    val context = LocalContext.current
    val storageInfo = remember { fetchStorageInfo(context) }
    val memoryInfo = remember { fetchMemoryInfo(context) }

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
                onClick = { onToggleOverlay() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if( isAppOverlaying ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAppOverlaying) "Stop Overlay" else "Start Overlay",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }


            Row(
                verticalAlignment = Alignment.CenterVertically,
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
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = if (isAppRecording) "Stop Recording" else "Start Recording",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Switch(
                    onCheckedChange = { requestPermission() },
                    checked = hasPermission,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Shizuku Service: $isBinderAlive"
            )
            Text(
                text = "Permission: $hasPermission"
            )
            Text(
                text = "Target App: $targetQuery"
            )
        }
    }
}


fun formatTimestamp(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(epochMillis))
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