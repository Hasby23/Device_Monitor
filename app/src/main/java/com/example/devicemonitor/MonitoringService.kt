package com.example.devicemonitor

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.IBinder
import android.view.Choreographer
import com.example.devicemonitor.db.AppDatabase
import com.example.devicemonitor.db.Record
import com.example.devicemonitor.db.RecordDao
import com.example.devicemonitor.overlay.OverlayManager
import com.example.devicemonitor.repository.AppRepository
import com.example.devicemonitor.repository.RecordedData
import com.example.devicemonitor.shizuku.ShizukuManager
import com.example.devicemonitor.shizuku.SurfaceFlingerParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

class MonitoringService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val overlayManager = OverlayManager()
    private val shizukuManager = ShizukuManager()

    private val listOfTimestamp = mutableListOf<Long>()
    private val listOfBatteryTemperature = mutableListOf<Float>()
    private val listOfFps = mutableListOf<Int>()
    private val listOfBatteryPercentage = mutableListOf<Int>()
    private val listOfAppName = mutableListOf<String>()

    private lateinit var dao: RecordDao

    override fun onCreate() {
        super.onCreate()

        dao = AppDatabase.getDatabase(applicationContext).recordDao()
        shizukuManager.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.cancel()
        shizukuManager.stop()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_COUNTING -> startMeasuring()
            ACTION_START_OVERLAYING -> startOverlaying()
            ACTION_STOP_OVERLAYING -> stopOverlaying()
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_REQUEST_SHIZUKU_PERMISSION -> requestShizukuPermission()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (AppRepository.isOverlaying.value) {
            overlayManager.clampToCurrentScreen()
        }
    }

    var frameCount = 0
    val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private var serviceJob: Job? = null
    private fun startMeasuring() {
        if (serviceJob != null) return

        Choreographer.getInstance().postFrameCallback(frameCallback)

        serviceJob = serviceScope.launch {
            val interval = 1000L

            while (isActive) {
                var dataCaptured: RecordedData? = null

                val executionTime = measureTimeMillis {
                    dataCaptured = withTimeoutOrNull(950L.milliseconds) {
                        fetchData()
                    }
                }

                if (dataCaptured == null) {
                    dataCaptured = RecordedData(
                        fps = 0,
                        batteryPercentage = 0,
                        batteryTemperature = 0.0f,
                        appName = "No Data Captured"
                    )
                }

                AppRepository.updateValue(dataCaptured)

                val timeToDelay = interval - executionTime
                if (timeToDelay > 0) {
                    delay(timeToDelay.milliseconds)
                }

                if (AppRepository.isRecording.value) {
                    listOfTimestamp.add(System.currentTimeMillis())
                    listOfFps.add(AppRepository.fps.value)
                    listOfBatteryPercentage.add(AppRepository.batteryPercentage.value)
                    listOfBatteryTemperature.add(AppRepository.batteryTemperature.value)
                    listOfAppName.add(AppRepository.targetQuery.value)
                }
            }
        }
    }

    private suspend fun fetchData() : RecordedData {
        val (currentBatteryPercentage, currentBatteryTemp) = readBatteryPercentAndTemp()

        if (AppRepository.hasPermission.value) {
            shizukuManager.runShizukuCommand("dumpsys SurfaceFlinger --timestats -clear -enable")
            val targetApp = shizukuManager.runShizukuCommand("dumpsys window | grep -Eo 'mCurrentFocus=Window\\{[a-f0-9]+ [^ ]+ [^/]+' | grep -Eo '[^ ]+$'").filterNot { it.isWhitespace() }

            val result = shizukuManager.runShizukuCommand("dumpsys SurfaceFlinger --timestats -dump")
            shizukuManager.runShizukuCommand("dumpsys SurfaceFlinger --timestats -disable")

            val currentFps = SurfaceFlingerParser.parseTotalFrames(result, targetApp)?.toInt() ?: 0

            frameCount = 0

            return RecordedData(
                fps = currentFps,
                batteryPercentage = currentBatteryPercentage,
                batteryTemperature = currentBatteryTemp ?: 0.0f,
                appName = targetApp
            )
        } else {
            val currentFps = frameCount
            val targetApp = "Shizuku Not Active"

            frameCount = 0

            return RecordedData(
                fps = currentFps,
                batteryPercentage = currentBatteryPercentage,
                batteryTemperature = currentBatteryTemp ?: 0.0f,
                appName = targetApp
            )
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

    fun startRecording() {
        listOfTimestamp.clear()
        listOfFps.clear()
        listOfBatteryPercentage.clear()
        listOfBatteryTemperature.clear()
        listOfAppName.clear()

        AppRepository.setRecord(true)
    }

    fun stopRecording() {
        AppRepository.setRecord(false)

        if (listOfTimestamp.isEmpty()) return

        val recordSession = Record(
            timestamp = listOfTimestamp.toList(),
            batteryTemperature = listOfBatteryTemperature.toList(),
            batteryPercent = listOfBatteryPercentage.toList(),
            fps = listOfFps.toList(),
            appName = listOfAppName.toList()
        )
        persistenceScope.launch {
            dao.insert(recordSession)
        }

        listOfTimestamp.clear()
        listOfFps.clear()
        listOfBatteryPercentage.clear()
        listOfBatteryTemperature.clear()
        listOfAppName.clear()
    }

    fun startOverlaying() {
        overlayManager.startOverlaying(this)
        AppRepository.setOverlay(true)
    }
    fun stopOverlaying() {
        overlayManager.stopOverlaying()
        AppRepository.setOverlay(false)
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    companion object {
        const val ACTION_START_COUNTING = "ACTION_START_COUNTING"

        const val ACTION_START_OVERLAYING = "ACTION_START_OVERLAYING"
        const val ACTION_STOP_OVERLAYING = "ACTION_STOP_OVERLAYING"

        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"

        const val ACTION_REQUEST_SHIZUKU_PERMISSION = "ACTION_REQUEST_SHIZUKU_PERMISSION"
    }
}
