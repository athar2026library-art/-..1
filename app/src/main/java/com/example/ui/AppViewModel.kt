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
import com.example.data.FeedbackDraft
import com.example.data.FeedbackItem
import com.example.data.Zekr
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

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

    // AI-only responses
    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    // Status / toast-like messages (sync, login, etc.)
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    private val _userSignedIn = MutableStateFlow(authRepository.getCurrentUser() != null)
    val userSignedIn: StateFlow<Boolean> = _userSignedIn.asStateFlow()

    val myFeedback: StateFlow<List<FeedbackItem>> = firestoreRepository.observeMyFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadFeedbackCount: StateFlow<Int> = myFeedback
        .let { flow -> kotlinx.coroutines.flow.flow { flow.collect { emit(it.count { item -> item.replyUnread }) } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ---- Debounced tasbeeh ----
    private val pendingTasbeeh = AtomicInteger(0)
    private var flushJob: Job? = null

    init {
        viewModelScope.launch {
            progressRepository.initTodayProgress()
        }
    }

    /**
     * Batches rapid taps. Flushes to Room at most every ~1.5 seconds
     * or when [flushPendingTasbeeh] is called (e.g. on screen leave).
     */
    fun addTasbeeh(count: Int = 1) {
        pendingTasbeeh.addAndGet(count)
        if (flushJob?.isActive != true) {
            flushJob = viewModelScope.launch {
                delay(1500)
                flushPendingTasbeeh()
            }
        }
    }

    fun flushPendingTasbeeh() {
        val toWrite = pendingTasbeeh.getAndSet(0)
        if (toWrite > 0) {
            viewModelScope.launch {
                progressRepository.addTasbeeh(toWrite)
            }
        }
        flushJob?.cancel()
        flushJob = null
    }

    override fun onCleared() {
        flushPendingTasbeeh()
        super.onCleared()
    }

    fun completeSabah() {
        flushPendingTasbeeh()
        viewModelScope.launch {
            progressRepository.completeSabah()
        }
    }

    fun completeMasaa() {
        flushPendingTasbeeh()
        viewModelScope.launch {
            progressRepository.completeMasaa()
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

    fun clearAiResponse() { _aiResponse.value = "" }
    fun clearStatusMessage() { _statusMessage.value = "" }
    
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

    fun markFeedbackReplyRead(feedbackId: String) {
        viewModelScope.launch { firestoreRepository.markFeedbackReplyRead(feedbackId) }
    }

    fun submitFeedback(draft: FeedbackDraft) {
        viewModelScope.launch {
            if (authRepository.getCurrentUser() == null) {
                val signedIn = authRepository.signInWithGoogle()
                _userSignedIn.value = signedIn
                if (!signedIn) { _statusMessage.value = "يجب تسجيل الدخول لإرسال الطلب."; return@launch }
            }
            val result = firestoreRepository.submitFeedback(draft, draft.attachmentUri)
            _statusMessage.value = if (result.isSuccess) "تم إرسال طلبك بنجاح، ويمكنك متابعة حالته من هنا." else "تعذر إرسال الطلب. حاول مرة أخرى."
        }
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

    suspend fun fetchPublishedAzkar(category: String): List<Zekr> =
        firestoreRepository.fetchPublishedAzkar(category)

    fun observePublishedAzkar(category: String): Flow<List<Zekr>> =
        firestoreRepository.observePublishedAzkar(category)
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
