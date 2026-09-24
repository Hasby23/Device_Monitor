package com.example.devicemonitor

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.example.devicemonitor.db.RecordViewModel
import com.example.devicemonitor.repository.AppRepository
import com.example.devicemonitor.screen.ResultScreen
import com.example.devicemonitor.screen.StartingScreen
import com.example.devicemonitor.ui.theme.DeviceMonitorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RecordViewModel by viewModels()
    val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, MonitoringService::class.java).apply {
                action = MonitoringService.ACTION_START_OVERLAYING
            }
            startService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_START_COUNTING
        }
        startService(intent)

        enableEdgeToEdge()
        setContent {
            DeviceMonitorTheme {
                var selectIndex by remember { mutableIntStateOf(0) }
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
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
                            onToggleRecording = { onRecordButtonPressed() },
                            onToggleOverlay = { onOverlayButtonPressed() },
                            requestPermission = { requestShizukuPermission() },
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> ResultScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun onRecordButtonPressed() {
        val intent = Intent(this, MonitoringService::class.java)
        if (AppRepository.isRecording.value) {
            intent.action = MonitoringService.ACTION_STOP_RECORDING
        } else {
            intent.action = MonitoringService.ACTION_START_RECORDING
        }
        startService(intent)
    }

    private fun onOverlayButtonPressed() {
        if (AppRepository.isOverlaying.value) {
            val intent = Intent(this, MonitoringService::class.java).apply {
                action = MonitoringService.ACTION_STOP_OVERLAYING
            }
            startService(intent)
        } else if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, MonitoringService::class.java).apply {
                action = MonitoringService.ACTION_START_OVERLAYING
            }
            startService(intent)
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )

        overlayPermissionLauncher.launch(intent)
    }

    private fun requestShizukuPermission() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_REQUEST_SHIZUKU_PERMISSION
        }
        startService(intent)
    }
}