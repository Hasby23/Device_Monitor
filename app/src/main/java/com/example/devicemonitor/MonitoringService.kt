package com.example.devicemonitor

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.Choreographer
import com.example.devicemonitor.overlay.OverlayClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import kotlin.time.Duration.Companion.milliseconds


class MonitoringService : Service() {
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder
    inner class LocalBinder : Binder() {
        fun getService(): MonitoringService = this@MonitoringService
    }

    private val overlay = OverlayClass()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var reportJob: Job? = null
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val listOfTimestamp = mutableListOf<Long>()
    val listOfBatteryTemperature = mutableListOf<Float>()
    val listOfFps = mutableListOf<Int>()
    val listOfBatteryPercentage = mutableListOf<Int>()
    private lateinit var dao: RecordDao


    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _isBinderAlive.value = true
        checkPermission()
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isBinderAlive.value = false
        _hasPermission.value = false
    }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            _hasPermission.value = (grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onCreate() {
        super.onCreate()
        dao = AppDatabase.getDatabase(applicationContext).recordDao()

        _isBinderAlive.value = Shizuku.pingBinder()
        checkPermission()

        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        startMeasuring()
    }

    override fun onDestroy() {
        super.onDestroy()

        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)

        scope.cancel()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isOverlaying.value) {
            overlay.clampToCurrentScreen()
        }
    }

    private var frameCount = 0
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
    private fun startMeasuring() {
        if (reportJob != null) return

        Choreographer.getInstance().postFrameCallback(frameCallback)

        reportJob = scope.launch {
            while (isActive) {
                Log.d("MyTag", "start loop")
                if (_hasPermission.value) {
                    Shizuku.newProcess(
                        arrayOf("sh", "-c", "dumpsys SurfaceFlinger --timestats -clear -enable"),
                        null,
                        null
                    )
                    delay(910.milliseconds)

                    val (currentBatteryPercentage, currentBatteryTemp) = readBatteryPercentAndTemp()
                    _batteryPercentage.value = currentBatteryPercentage
                    _batteryTemperature.value = currentBatteryTemp ?: 0.0f

                    val result = runShizukuCommand("dumpsys SurfaceFlinger --timestats -dump")
                    Shizuku.newProcess(
                        arrayOf("sh", "-c", "dumpsys SurfaceFlinger --timestats -disable"),
                        null,
                        null
                    )
                    _targetQuery.value = runShizukuCommand("dumpsys window | grep -Eo 'mCurrentFocus=Window\\{[a-f0-9]+ [^ ]+ [^/]+' | grep -Eo '[^ ]+$'").filterNot { it.isWhitespace() }

                    val desiredOutput = SurfaceFlingerParser.parseTotalFrames(result, _targetQuery.value)
                    _fps.value = desiredOutput?.toInt() ?: 0
                } else {
                    delay(1000.milliseconds)

                    val (currentBatteryPercentage, currentBatteryTemp) = readBatteryPercentAndTemp()
                    _batteryPercentage.value = currentBatteryPercentage
                    _batteryTemperature.value = currentBatteryTemp ?: 0.0f

                    val currentFps = frameCount
                    frameCount = 0
                    _fps.value = currentFps
                }
                if(_isRecording.value) {
                    listOfTimestamp.add(System.currentTimeMillis())
                    listOfFps.add(_fps.value)
                    listOfBatteryPercentage.add(_batteryPercentage.value)
                    listOfBatteryTemperature.add(_batteryTemperature.value)
                }
                Log.d("MyTag", "finish loop")
            }
        }
    }

    private fun readBatteryPercentAndTemp(): Pair<Int,Float?> {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 1
        val percentage = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale) * 100).toInt()
        } else {
            -1
        }

        val tenthsOfDegree = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

        return Pair(percentage, if (tenthsOfDegree < 0) null else tenthsOfDegree / 10.0f)
    }

    fun manageRecording() {
        if (_isRecording.value) {
            _isRecording.value = false

            if (listOfTimestamp.isEmpty()) return

            val record = Record(
                timestamp = listOfTimestamp.toList(),
                batteryTemperature = listOfBatteryTemperature.toList(),
                batteryPercent = listOfBatteryPercentage.toList(),
                fps = listOfFps.toList()
            )
            persistenceScope.launch {
                dao.insert(record)
            }

            listOfTimestamp.clear()
            listOfFps.clear()
            listOfBatteryPercentage.clear()
            listOfBatteryTemperature.clear()

        } else {
            listOfTimestamp.clear()
            listOfFps.clear()
            listOfBatteryPercentage.clear()
            listOfBatteryTemperature.clear()

            _isRecording.value = true
        }
    }

    fun startOverlaying() {
        if (_isOverlaying.value) return
        overlay.startOverlaying(this)
        _isOverlaying.value = true
    }
    fun stopOverlaying() {
        if (!_isOverlaying.value) return
        overlay.stopOverlaying()
        _isOverlaying.value = false
    }

    fun checkPermission(): Boolean {
        if (Shizuku.isPreV11()) {
            _hasPermission.value = false
            return false
        }
        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
        _hasPermission.value = granted
        return granted
    }

    fun requestPermission() {
        if (!Shizuku.isPreV11()) {
            try {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            } catch (e: Exception) {
                _commandOutput.value = "Error requesting permission: ${e.localizedMessage}"
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun runShizukuCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            buildString {
                if (output.isNotBlank()) append(output)
                if (error.isNotBlank()) {
                    if (isNotEmpty()) append("\n-- Error Stream --\n")
                    append(error)
                }
                if (isEmpty()) append("Command finished with exit code $exitCode (No output)")
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    companion object {
        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

        private val _fps = MutableStateFlow(0)
        val fps: StateFlow<Int> = _fps.asStateFlow()

        private val _batteryPercentage = MutableStateFlow(0)
        val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

        private val _batteryTemperature = MutableStateFlow(0f)
        val batteryTemperature: StateFlow<Float> = _batteryTemperature.asStateFlow()

        private val _isOverlaying = MutableStateFlow(false)
        val isOverlaying: StateFlow<Boolean> = _isOverlaying.asStateFlow()

        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
        private val _isBinderAlive = MutableStateFlow(false)
        val isBinderAlive: StateFlow<Boolean> = _isBinderAlive.asStateFlow()
        private val _hasPermission = MutableStateFlow(false)
        val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

        private val _commandOutput = MutableStateFlow("")
        val commandOutput: StateFlow<String> = _commandOutput.asStateFlow()
        private val _targetQuery = MutableStateFlow("")
        val targetQuery: StateFlow<String> = _targetQuery.asStateFlow()
    }
}
