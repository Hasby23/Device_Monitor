package com.example.devicemonitor

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.material3.MaterialTheme
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
import com.example.devicemonitor.ui.theme.DeviceMonitorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RecordViewModel by viewModels()
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            showOverlay()
        }
    }
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
                            onToggleRecording = { onButtonPressed() },
                            onToggleOverlay = { onOverlayButtonPressed() },
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

    private fun onOverlayButtonPressed() {
        if (MonitoringService.isOverlaying.value) {
            hideOverlay()
        } else if (Settings.canDrawOverlays(this)) {
            showOverlay()
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

    private fun showOverlay() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_START_OVERLAY
        }
        startService(intent)
    }

    private fun hideOverlay() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_STOP_OVERLAY
        }
        startService(intent)
    }
    private fun clampOverlay() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_CLAMP_OVERLAY
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (MonitoringService.isOverlaying.value){
            hideOverlay()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (MonitoringService.isOverlaying.value) {
            clampOverlay()
        }
    }
}