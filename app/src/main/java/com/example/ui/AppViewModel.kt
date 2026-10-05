package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.AuthRepository
import com.example.data.FeedbackDraft
import com.example.data.FeedbackItem
import com.example.data.FirestoreRepository
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.data.UserProgress
import com.example.data.Zekr
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoPlayEnabled: StateFlow<Boolean> = settingsRepository.autoPlayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastReadCategory: StateFlow<String> = settingsRepository.lastReadCategoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val lastReadIndex: StateFlow<Int> = settingsRepository.lastReadIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastReadRemaining: StateFlow<Int> = settingsRepository.lastReadRemainingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                "السلام عليكم، كيف يمكنني مساعدتك اليوم؟ (أذكار، أدعية، فضل ذكر معين...)",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _userSignedIn = MutableStateFlow(authRepository.getCurrentUser() != null)
    val userSignedIn: StateFlow<Boolean> = _userSignedIn.asStateFlow()

    val myFeedback: StateFlow<List<FeedbackItem>> = firestoreRepository.observeMyFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadFeedbackCount: StateFlow<Int> = myFeedback
        .map { list -> list.count { it.replyUnread } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var pendingTasbeeh = 0
    private var flushJob: Job? = null

    init {
        viewModelScope.launch {
            progressRepository.initTodayProgress()
        }
    }

    fun addTasbeeh(count: Int = 1) {
        pendingTasbeeh += count
        flushJob?.cancel()
        flushJob = viewModelScope.launch {
            delay(800)
            flushTasbeeh()
        }
    }

    private suspend fun flushTasbeeh() {
        val toWrite = pendingTasbeeh
        pendingTasbeeh = 0
        if (toWrite > 0) {
            progressRepository.addTasbeeh(toWrite)
        }
        flushJob = null
    }

    fun flushPendingTasbeeh() {
        viewModelScope.launch { flushTasbeeh() }
    }

    override fun onCleared() {
        flushPendingTasbeeh()
        super.onCleared()
    }

    fun completeSabah() {
        flushPendingTasbeeh()
        viewModelScope.launch { progressRepository.completeSabah() }
    }

    fun completeMasaa() {
        flushPendingTasbeeh()
        viewModelScope.launch { progressRepository.completeMasaa() }
    }

    fun saveLastReadState(category: String, index: Int, remaining: Int) {
        viewModelScope.launch {
            settingsRepository.saveLastReadState(category, index, remaining)
        }
    }

    fun clearLastReadState() {
        viewModelScope.launch { settingsRepository.clearLastReadState() }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { settingsRepository.setFontSize(size) }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkMode(isDark) }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibration(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun setAutoDnd(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoDnd(enabled) }
    }

    fun setHideVirtues(hide: Boolean) {
        viewModelScope.launch { settingsRepository.setHideVirtues(hide) }
    }

    fun setHideSources(hide: Boolean) {
        viewModelScope.launch { settingsRepository.setHideSources(hide) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            // ضروري لوصول بث لوحة الإدارة (فلتر notificationsEnabled في Functions)
            firestoreRepository.updateNotificationPrefs(enabled)
        }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlay(enabled) }
    }

    fun sendChatMessage(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _isLoadingAi.value) return
        _isLoadingAi.value = true
        _chatMessages.update {
            it + ChatMessage(message, isUser = true) +
                ChatMessage("جاري البحث...", isUser = false, isLoading = true)
        }
        viewModelScope.launch {
            val reply = try {
                aiRepository.ask(message)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                "تعذر الاتصال. يرجى التأكد من اتصالك بالإنترنت للميزات الذكية 🌐"
            }
            _chatMessages.update { list ->
                list.filterNot { it.isLoading } + ChatMessage(reply, isUser = false)
            }
            _isLoadingAi.value = false
        }
    }

    fun clearStatusMessage() { _statusMessage.value = "" }

    fun signIn(context: Context) {
        viewModelScope.launch {
            val success = authRepository.signInWithGoogle(context)
            if (success) {
                _userSignedIn.value = true
                _statusMessage.value = "تم تسجيل الدخول بنجاح!"
                val enabled = settingsRepository.notificationsEnabledFlow.first()
                firestoreRepository.saveFcmToken(notificationsEnabled = enabled)
            } else {
                _statusMessage.value = "فشل تسجيل الدخول أو تم إلغاؤه."
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _userSignedIn.value = false
            _statusMessage.value = "تم تسجيل الخروج."
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _statusMessage.value = "جاري حذف الحساب..."
            try {
                FirebaseFunctions.getInstance("europe-west1")
                    .getHttpsCallable("deleteAccount")
                    .call()
                    .await()
                progressRepository.clearLocalProgress()
                progressRepository.initTodayProgress()
                authRepository.signOut()
                _userSignedIn.value = false
                _statusMessage.value = "تم حذف الحساب نهائياً."
            } catch (e: FirebaseFunctionsException) {
                _statusMessage.value = when (e.code) {
                    FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                        "سجّل الدخول مجدداً ثم أعد المحاولة."
                    else -> "تعذر حذف الحساب. حاول لاحقاً."
                }
            } catch (_: Exception) {
                _statusMessage.value = "تعذر حذف الحساب. تحقق من الاتصال."
            }
        }
    }

    fun markFeedbackReplyRead(feedbackId: String) {
        viewModelScope.launch { firestoreRepository.markFeedbackReplyRead(feedbackId) }
    }

    fun submitFeedback(draft: FeedbackDraft, context: Context?) {
        viewModelScope.launch {
            if (authRepository.getCurrentUser() == null) {
                if (context == null) {
                    _statusMessage.value = "يجب تسجيل الدخول لإرسال الطلب."
                    return@launch
                }
                val signedIn = authRepository.signInWithGoogle(context)
                _userSignedIn.value = signedIn
                if (!signedIn) {
                    _statusMessage.value = "يجب تسجيل الدخول لإرسال الطلب."
                    return@launch
                }
                val enabled = settingsRepository.notificationsEnabledFlow.first()
                firestoreRepository.saveFcmToken(notificationsEnabled = enabled)
            }
            val result = firestoreRepository.submitFeedback(draft, draft.attachmentUri)
            _statusMessage.value = if (result.isSuccess) {
                "تم إرسال طلبك بنجاح، ويمكنك متابعة حالته من هنا."
            } else {
                "تعذر إرسال الطلب. حاول مرة أخرى."
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _statusMessage.value = "جاري المزامنة مع السحابة..."
            val remote = firestoreRepository.fetchProgress()
            if (remote == null) {
                _statusMessage.value = "تعذرت المزامنة، تأكد من اتصالك وتسجيل الدخول 🌐"
                return@launch
            }
            progressRepository.syncProgress(remote)
            val local = progressRepository.getAllProgress().ifEmpty {
                progressRepository.getRecentProgress().first()
            }
            val ok = firestoreRepository.backupProgress(local)
            _statusMessage.value =
                if (ok) "تمت مزامنة البستان بنجاح! 🌴" else "تعذر رفع التقدم إلى السحابة"
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
            return AppViewModel(
                progressRepository,
                settingsRepository,
                aiRepository,
                authRepository,
                firestoreRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
