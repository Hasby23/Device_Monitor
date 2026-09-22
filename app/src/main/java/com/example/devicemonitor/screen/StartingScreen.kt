package com.example.devicemonitor.screen

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.devicemonitor.repository.AppRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val fps by AppRepository.fps.collectAsStateWithLifecycle()
    val batteryPercentage by AppRepository.batteryPercentage.collectAsStateWithLifecycle()
    val batteryTemperature by AppRepository.batteryTemperature.collectAsStateWithLifecycle()

    val isAppOverlaying by AppRepository.isOverlaying.collectAsStateWithLifecycle()
    val isAppRecording by AppRepository.isRecording.collectAsStateWithLifecycle()

    // SHIZUKU
    val hasPermission by AppRepository.hasPermission.collectAsStateWithLifecycle()
    val isBinderAlive by AppRepository.isBinderAlive.collectAsStateWithLifecycle()
    val targetQuery by AppRepository.targetQuery.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val storageInfo = remember { fetchStorageInfo(context) }
    val memoryInfo = remember { fetchMemoryInfo(context) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(4.dp)
                )
                Text(
                    text = "SoC: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Build.SOC_MODEL
                    } else {
                        Build.HARDWARE
                    }}",
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Android ${Build.VERSION.RELEASE} ",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                )
            }
        }

        CardRow(
            text1 = "Battery: ${if (batteryPercentage >= 0) "${batteryPercentage}%" else "Unavailable"}",
            text2 = "Temperature: $batteryTemperature °C",
            text3 = "FPS:",
            text4 = "$fps"
        )
        CardRow(
            text1 = "Storage used: ${storageInfo.usedStorage}",
            text2 = "Storage total: ${storageInfo.totalStorage}",
            text3 = "Ram used: ${memoryInfo.usedRam}",
            text4 = "Ram Total: ${memoryInfo.totalRam}"
        )

        Column(modifier = Modifier.fillMaxWidth()) {
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


            Row(verticalAlignment = Alignment.CenterVertically) {
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

        Column(modifier = Modifier.fillMaxWidth()) {
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

@Composable
fun CardRow(
    text1: String,
    text2: String,
    text3: String,
    text4: String,
){
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = text1,
                    modifier = Modifier.padding(4.dp)
                )
                Text(
                    text = text2,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = text3,
                    modifier = Modifier.padding(4.dp)
                )
                Text(
                    text = text4,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

fun formatTimestamp(epochMillis: Long, noTime: Boolean = false): String {
    if (noTime) {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }
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