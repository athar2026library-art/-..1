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

    private val _aiResponse = MutableStateFlow<String>("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()
    
    private val _aiImageBase64 = MutableStateFlow<String?>(null)
    val aiImageBase64: StateFlow<String?> = _aiImageBase64.asStateFlow()

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
    
    fun askAi(question: String) {
        viewModelScope.launch {
            _aiResponse.value = "جاري البحث..."
            _aiResponse.value = aiRepository.askScholar(question)
        }
    }
    
    fun generateAiImage(prompt: String) {
        viewModelScope.launch {
            _aiResponse.value = "جاري رسم الخلفية..."
            val image = aiRepository.generateImage(prompt)
            if (image != null) {
                _aiImageBase64.value = image
                _aiResponse.value = "تم بنجاح!"
            } else {
                _aiResponse.value = "فشل توليد الصورة."
            }
        }
    }
    
    fun generateAiVideo(prompt: String) {
        viewModelScope.launch {
            _aiResponse.value = "جاري إنشاء الفيديو..."
            val result = aiRepository.generateVideo(prompt)
            _aiResponse.value = "نتيجة الفيديو: $result"
        }
    }
    
    fun signIn() {
        viewModelScope.launch {
            val success = authRepository.signInWithGoogle()
            if (success) {
                _userSignedIn.value = true
                _aiResponse.value = "تم تسجيل الدخول بنجاح!"
            } else {
                _aiResponse.value = "فشل تسجيل الدخول. تأكد من إعدادات Firebase."
            }
        }
    }
    
    fun signOut() {
        authRepository.signOut()
        _userSignedIn.value = false
        _aiResponse.value = "تم تسجيل الخروج."
    }
    
    fun backupData() {
        viewModelScope.launch {
            _aiResponse.value = "جاري المزامنة مع السحابة..."
            firestoreRepository.backupProgress(recentProgress.value)
            _aiResponse.value = "تمت مزامنة البستان بنجاح! 🌴"
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
