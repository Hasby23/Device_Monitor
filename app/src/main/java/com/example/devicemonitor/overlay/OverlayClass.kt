package com.example.devicemonitor.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
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
import com.example.devicemonitor.MonitoringService

class OverlayClass() {
    private var windowManager: WindowManager? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var params: WindowManager.LayoutParams? = null
    private var composeView: ComposeView? = null

    val isShowing: Boolean
        get() = composeView != null

    @SuppressLint("ClickableViewAccessibility")
    fun startOverlaying(context: Context) {
        if (isShowing) return

        windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager

        val owner = OverlayLifecycleOwner().apply {
            performRestore(null)
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner = owner

        val layoutParams = buildLayoutParams()
        params = layoutParams

        val view = ComposeView(context)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setContent {
            OverlayReadoutView(
                fps = MonitoringService.fps.collectAsState().value,
                batteryTemp = MonitoringService.batteryTemperature.collectAsState().value,
                batteryPercent = MonitoringService.batteryPercentage.collectAsState().value,
                appName = MonitoringService.targetQuery.collectAsState().value
            )
        }
        view.setOnTouchListener(DragToMoveListener(layoutParams))
        composeView = view

        windowManager?.addView(view, layoutParams)
    }

    fun stopOverlaying() {
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

    private inner class DragToMoveListener(private val params: WindowManager.LayoutParams) : View.OnTouchListener {
        private var startParamX = 0
        private var startParamY = 0
        private var startTouchX = 0f
        private var startTouchY = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {

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
}