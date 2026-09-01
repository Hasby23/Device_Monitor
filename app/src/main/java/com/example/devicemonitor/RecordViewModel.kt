package com.example.devicemonitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).recordDao()

    val records: StateFlow<List<Record>> = dao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRecord(timestamp: List<Long>, batteryTemperature: List<Float>, batteryPercent: List<Int>, fps: List<Int>) {
        val record = Record(
            timestamp = timestamp,
            batteryTemperature = batteryTemperature,
            batteryPercent = batteryPercent,
            fps = fps
        )

        viewModelScope.launch {
            dao.insert(record)
        }
    }

    fun deleteRecord(record: Record) {
        viewModelScope.launch {
            dao.delete(record)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}