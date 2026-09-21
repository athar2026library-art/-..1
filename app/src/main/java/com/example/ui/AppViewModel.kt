package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.data.UserProgress
import com.example.data.AiRepository
import com.example.data.AuthRepository
import com.example.data.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AiRepository,
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
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

    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoDnd: StateFlow<Boolean> = settingsRepository.autoDndFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hideVirtues: StateFlow<Boolean> = settingsRepository.hideVirtuesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hideSources: StateFlow<Boolean> = settingsRepository.hideSourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastReadCategory: StateFlow<String> = settingsRepository.lastReadCategoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
        
    val lastReadIndex: StateFlow<Int> = settingsRepository.lastReadIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val lastReadRemaining: StateFlow<Int> = settingsRepository.lastReadRemainingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // AI response is only for actual AI replies
    private val _aiResponse = MutableStateFlow<String>("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()
    
    // Separate channel for status / toast-like messages (sync, login, errors)
    private val _statusMessage = MutableStateFlow<String>("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    private val _userSignedIn = MutableStateFlow(authRepository.getCurrentUser() != null)
    val userSignedIn: StateFlow<Boolean> = _userSignedIn.asStateFlow()

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
    
    fun saveLastReadState(category: String, index: Int, remaining: Int) {
        viewModelScope.launch {
            settingsRepository.saveLastReadState(category, index, remaining)
        }
    }

    fun clearLastReadState() {
        viewModelScope.launch {
            settingsRepository.clearLastReadState()
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
    
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
        }
    }

    fun setAutoDnd(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoDnd(enabled)
        }
    }

    fun setHideVirtues(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideVirtues(hide)
        }
    }

    fun setHideSources(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideSources(hide)
        }
    }
    
    fun explainZekr(zekr: String) {
        viewModelScope.launch {
            _isLoadingAi.value = true
            _aiResponse.value = ""
            try {
                _aiResponse.value = aiRepository.explainZekr(zekr)
            } catch (e: Exception) {
                _aiResponse.value = "تعذر الاتصال. يرجى التأكد من اتصالك بالإنترنت للميزات الذكية 🌐"
            } finally {
                _isLoadingAi.value = false
            }
        }
    }

    fun suggestZekr(feeling: String) {
        viewModelScope.launch {
            _isLoadingAi.value = true
            _aiResponse.value = ""
            try {
                _aiResponse.value = aiRepository.suggestZekrForFeeling(feeling)
            } catch (e: Exception) {
                _aiResponse.value = "تعذر الاتصال. يرجى التأكد من اتصالك بالإنترنت للميزات الذكية 🌐"
            } finally {
                _isLoadingAi.value = false
            }
        }
    }
    
    fun clearAiResponse() {
        _aiResponse.value = ""
    }
    
    fun clearStatusMessage() {
        _statusMessage.value = ""
    }
    
    fun signIn() {
        viewModelScope.launch {
            val success = authRepository.signInWithGoogle()
            if (success) {
                _userSignedIn.value = true
                _statusMessage.value = "تم تسجيل الدخول بنجاح!"
            } else {
                _statusMessage.value = "فشل تسجيل الدخول. تأكد من إعدادات Firebase."
            }
        }
    }
    
    fun signOut() {
        authRepository.signOut()
        _userSignedIn.value = false
        _statusMessage.value = "تم تسجيل الخروج."
    }
    
    fun syncData() {
        viewModelScope.launch {
            _statusMessage.value = "جاري المزامنة مع السحابة..."
            try {
                val remoteProgress = firestoreRepository.fetchProgress()
                progressRepository.syncProgress(remoteProgress)
                firestoreRepository.backupProgress(recentProgress.value)
                _statusMessage.value = "تمت مزامنة البستان بنجاح! 🌴"
            } catch (e: Exception) {
                _statusMessage.value = "تعذرت المزامنة، تأكد من اتصالك بالإنترنت 🌐"
            }
        }
    }
}

class AppViewModelFactory(
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AiRepository,
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(progressRepository, settingsRepository, aiRepository, authRepository, firestoreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
