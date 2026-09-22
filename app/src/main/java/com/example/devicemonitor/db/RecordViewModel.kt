package com.example.devicemonitor.db

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).recordDao()

    val records: StateFlow<List<Record>> = dao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isLoading: StateFlow<Boolean> = dao.getAllRecords()
        .map { false }
        .onStart { emit(true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun deleteRecord(record: Record) {
        viewModelScope.launch {
            dao.delete(record)
        }
    }
}