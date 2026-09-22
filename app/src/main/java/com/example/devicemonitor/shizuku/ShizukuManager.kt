package com.example.devicemonitor.shizuku

import android.content.pm.PackageManager
import com.example.devicemonitor.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class ShizukuManager {
    private val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        AppRepository.setBinder(true)
        checkPermission()
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        AppRepository.setBinder(false)
        AppRepository.setPermission(false)
    }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            AppRepository.setPermission(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }

    fun start() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun stop() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    fun checkPermission(): Boolean {
        if (Shizuku.isPreV11()) {
            AppRepository.setPermission(false)
            return false
        }
        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
        AppRepository.setPermission(granted)
        return granted
    }

    fun requestPermission() {
        if (!Shizuku.isPreV11()) {
            try {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            } catch (e: Exception) {
                AppRepository.setCommandOutput("Error requesting permission: ${e.localizedMessage}")
            }
        }
    }

    @Suppress("DEPRECATION")
    suspend fun runShizukuCommand(command: String): String = withContext(Dispatchers.IO) {
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
}