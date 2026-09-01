package com.example.devicemonitor

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.devicemonitor.overlay.OverlayLifecycleOwner
import com.example.devicemonitor.overlay.OverlayReadoutView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MonitoringService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var reportJob: Job? = null
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var frameCount = 0
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            Choreographer.getInstance().postFrameCallback(this)
        }
    }


    val listOfTimestamp = mutableListOf<Long>()
    val listOfBatteryTemperature = mutableListOf<Float>()
    val listOfFps = mutableListOf<Int>()
    val listOfBatteryPercentage = mutableListOf<Int>()
    private lateinit var dao: RecordDao

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var params: WindowManager.LayoutParams? = null

    /** True while the overlay window is currently on screen. */
    val isShowing: Boolean get() = composeView != null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isOverlaying.value) {
            clampToCurrentScreen()
        }
    }

    override fun onCreate() {
        super.onCreate()
        dao = AppDatabase.getDatabase(applicationContext).recordDao()

        startMeasuring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_START_OVERLAY -> startOverlaying()
            ACTION_STOP_OVERLAY -> stopOverlaying()
            ACTION_CLAMP_OVERLAY  -> clampToCurrentScreen()
        }
        return START_NOT_STICKY
    }

    private fun startMeasuring() {
        if (reportJob != null) return

        Choreographer.getInstance().postFrameCallback(frameCallback)

        reportJob = scope.launch {
            while (isActive) {
                delay(1000.milliseconds)
                val currentFps = frameCount
                frameCount = 0
                _fps.value = currentFps

                val currentBatteryPercentage = readBatteryPercentage()
                _batteryPercentage.value = currentBatteryPercentage

                val currentBatteryTemp = readBatteryTemperature()
                if (currentBatteryTemp != null) {
                    _batteryTemperature.value = currentBatteryTemp
                }

                if(_isRecording.value) {
                    listOfTimestamp.add(System.currentTimeMillis())
                    listOfFps.add(currentFps)
                    listOfBatteryPercentage.add(currentBatteryPercentage)
                    listOfBatteryTemperature.add(currentBatteryTemp ?: 0f)
                }
            }
        }
    }

    private fun readBatteryPercentage(): Int{
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 1
        val percentage = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale) * 100).toInt()
        } else {
            -1
        }
        return percentage
    }
    private fun readBatteryTemperature(): Float?{
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tenthsOfDegree = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return if (tenthsOfDegree < 0) null else tenthsOfDegree / 10.0f
    }

    private fun startRecording() {
        if (_isRecording.value) return

        listOfTimestamp.clear()
        listOfFps.clear()
        listOfBatteryPercentage.clear()
        listOfBatteryTemperature.clear()
        _isRecording.value = true
    }

    private fun stopRecording() {
        if (!_isRecording.value) return

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
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startOverlaying() {
        if (isShowing) return
        if (_isOverlaying.value) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val owner = OverlayLifecycleOwner().apply {
            performRestore(null)
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner = owner

        val layoutParams = buildLayoutParams()
        params = layoutParams

        val view = ComposeView(this)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setContent {
            OverlayReadoutView(
                fps = _fps.collectAsState().value,
                batteryTemp = _batteryTemperature.collectAsState().value,
                batteryPercent = _batteryPercentage.collectAsState().value
            )
        }
        view.setOnTouchListener(DragToMoveListener(layoutParams))
        composeView = view

        _isOverlaying.value = true

        windowManager?.addView(view, layoutParams)
    }

    fun stopOverlaying() {
        if (!_isOverlaying.value) return
        val view = composeView ?: return

        lifecycleOwner?.apply {
            handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        windowManager?.removeView(view)

        composeView = null
        lifecycleOwner = null
        params = null

        _isOverlaying.value = false
    }

    fun clampToCurrentScreen() {
        val view = composeView ?: return
        val layoutParams = params ?: return
        val (screenWidth, screenHeight) = realScreenSize()

        val maxX = (screenWidth - view.width).coerceAtLeast(0)
        val maxY = (screenHeight - view.height).coerceAtLeast(0)

        layoutParams.x = layoutParams.x.coerceIn(0, maxX)
        layoutParams.y = layoutParams.y.coerceIn(0, maxY)

        windowManager?.updateViewLayout(view, layoutParams)
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Gravity.TOP/START + explicit x/y makes the offset math for dragging simple:
        // x and y are just absolute pixel offsets from the top-left of the screen.
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
    }

    private fun realScreenSize(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    private inner class DragToMoveListener(
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        private var startParamX = 0
        private var startParamY = 0
        private var startTouchX = 0f
        private var startTouchY = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
//                fun isLandscape(context: Context): Boolean {
//                    return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
//                }
                MotionEvent.ACTION_DOWN -> {
                    startParamX = params.x
                    startParamY = params.y
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val (screenWidth, screenHeight) = realScreenSize()
                    val maxX = (screenWidth - v.width).coerceAtLeast(0)
                    val maxY = (screenHeight - v.height).coerceAtLeast(0)

                    val newX = startParamX + (event.rawX - startTouchX).toInt()
                    val newY = startParamY + (event.rawY - startTouchY).toInt()

                    params.x = newX.coerceIn(0, maxX)
                    params.y = newY.coerceIn(0, maxY)
                    windowManager?.updateViewLayout(v, params)
                    return true
                }
            }
            return false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        const val ACTION_START_RECORDING = "com.example.counterservice.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.counterservice.action.STOP_RECORDING"
        const val ACTION_START_OVERLAY = "com.example.counterservice.action.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.counterservice.action.STOP_OVERLAY"
        const val ACTION_CLAMP_OVERLAY = "com.example.counterservice.action.CLAMP_OVERLAY"

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
    }
}
