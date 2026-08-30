package com.example.devicemonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.devicemonitor.ui.theme.DeviceMonitorTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val viewModel: RecordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(Intent(this, MonitoringService::class.java))

        enableEdgeToEdge()
        setContent {
            DeviceMonitorTheme {
                var selectIndex by remember { mutableIntStateOf(0) }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            NavigationBarItem(
                                selected = selectIndex == 0,
                                onClick = { selectIndex = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectIndex == 1,
                                onClick = { selectIndex = 1 },
                                icon = { Icon(Icons.Default.Check, contentDescription = "Result") },
                                label = { Text("Result") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectIndex) {
                        0 -> StartingScreen(
                            viewModel,
                            onToggleRecording = { onButtonPressed() },
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> ResultScreen(
                            viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
    private fun onButtonPressed() {
        if (MonitoringService.isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }


    private fun startRecording() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_START_RECORDING
        }
        startService(intent)
    }

    private fun stopRecording() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_STOP_RECORDING
        }
        startService(intent)
    }
}


//@Preview(showBackground = true)
//@Composable
//fun StartingScreenPreview() {
//    DeviceMonitorTheme {
//        StartingScreen()
//    }
//}