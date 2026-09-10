package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.data.UserProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val todayProgress: StateFlow<UserProgress?> = progressRepository.getTodayProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentProgress: StateFlow<List<UserProgress>> = progressRepository.getRecentProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val fontSize: StateFlow<Float> = settingsRepository.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24f)
        
    val isDarkMode: StateFlow<Boolean> = settingsRepository.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isVibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            progressRepository.initTodayProgress()
        }
    }

    fun completeSabah() {
        viewModelScope.launch {
            progressRepository.completeSabah()
        }
    }

    fun completeMasaa() {
        viewModelScope.launch {
            progressRepository.completeMasaa()
        }
    }
    
    fun addTasbeeh(count: Int) {
        viewModelScope.launch {
            progressRepository.addTasbeeh(count)
        }
    }
    
    fun setFontSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.setFontSize(size)
        }
    }
    
    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(isDark)
        }
    }
    
    fun setVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibration(enabled)
        }
    }
}

class AppViewModelFactory(
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(progressRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
