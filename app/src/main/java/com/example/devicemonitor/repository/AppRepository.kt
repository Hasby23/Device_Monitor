package com.example.devicemonitor.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecordedData (
    val fps: Int,
    val batteryPercentage: Int,
    val batteryTemperature: Float,
    val appName: String,
)

object AppRepository {
    /* Overlay */
    private val _isOverlaying = MutableStateFlow(false)
    val isOverlaying: StateFlow<Boolean> = _isOverlaying.asStateFlow()

    /* Record */
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    /* Shizuku */
    private val _isBinderAlive = MutableStateFlow(false)
    val isBinderAlive: StateFlow<Boolean> = _isBinderAlive.asStateFlow()
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()
    private val _commandOutput = MutableStateFlow("")
    val commandOutput: StateFlow<String> = _commandOutput.asStateFlow()
    private val _targetQuery = MutableStateFlow("")
    val targetQuery: StateFlow<String> = _targetQuery.asStateFlow()

    /* Value */
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()
    private val _batteryPercentage = MutableStateFlow(0)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()
    private val _batteryTemperature = MutableStateFlow(0f)
    val batteryTemperature: StateFlow<Float> = _batteryTemperature.asStateFlow()

    fun setOverlay(overlayState: Boolean) {
        _isOverlaying.value = overlayState
    }

    fun setRecord(recordState: Boolean) {
        _isRecording.value = recordState
    }

    fun updateValue(recordedData: RecordedData) {
        _fps.value = recordedData.fps
        _batteryPercentage.value = recordedData.batteryPercentage
        _batteryTemperature.value = recordedData.batteryTemperature
        _targetQuery.value = recordedData.appName
    }

    fun setBinder(binderState: Boolean) {
        _isBinderAlive.value = binderState
    }

    fun setPermission(permissionState: Boolean) {
        _hasPermission.value = permissionState
    }

    fun setCommandOutput(output: String) {
        _commandOutput.value = output
    }
}