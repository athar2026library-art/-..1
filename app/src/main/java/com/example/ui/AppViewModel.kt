package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.AuthRepository
import com.example.data.AzkarCategory
import com.example.data.CategoryDefaults
import com.example.data.CustomWird
import com.example.data.FeedbackDraft
import com.example.data.FeedbackItem
import com.example.data.FirestoreRepository
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.data.UserProgress
import com.example.data.Zekr
import com.example.ui.theme.AccentTheme
import com.example.ui.theme.ReaderFont
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    val remindSabah: StateFlow<Boolean> = settingsRepository.remindSabahFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val remindMasaa: StateFlow<Boolean> = settingsRepository.remindMasaaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val remindSleep: StateFlow<Boolean> = settingsRepository.remindSleepFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val remindFriday: StateFlow<Boolean> = settingsRepository.remindFridayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val reminderSabahTime: StateFlow<String> = settingsRepository.reminderSabahTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "07:00")
    val reminderMasaaTime: StateFlow<String> = settingsRepository.reminderMasaaTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "18:00")
    val reminderSleepTime: StateFlow<String> = settingsRepository.reminderSleepTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "22:00")
    val reminderFridayTime: StateFlow<String> = settingsRepository.reminderFridayTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "09:00")

    val autoPlayEnabled: StateFlow<Boolean> = settingsRepository.autoPlayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val audioSpeed: StateFlow<Float> = settingsRepository.audioSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    val reciterId: StateFlow<String> = settingsRepository.reciterIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "mishary")

    val favorites: StateFlow<Set<Int>> = settingsRepository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val savedAiAzkar: StateFlow<List<String>> = settingsRepository.savedAiAzkarFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readerFont: StateFlow<ReaderFont> = settingsRepository.readerFontFlow
        .map { ReaderFont.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderFont.AMIRI)

    val allProgress: StateFlow<List<UserProgress>> = progressRepository.getAllProgressFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyDaysGoal: StateFlow<Int> = settingsRepository.weeklyGoalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val accentTheme: StateFlow<AccentTheme> = settingsRepository.accentFlow
        .map { AccentTheme.from(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentTheme.GOLD)

    val dailyTasbeehGoal: StateFlow<Int> = settingsRepository.dailyGoalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val tasbihTarget: StateFlow<Int> = settingsRepository.tasbihTargetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 33)

    val lastReadCategory: StateFlow<String> = settingsRepository.lastReadCategoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val lastReadIndex: StateFlow<Int> = settingsRepository.lastReadIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastReadRemaining: StateFlow<Int> = settingsRepository.lastReadRemainingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val customWirds: StateFlow<List<CustomWird>> = settingsRepository.customWirdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * تصنيفات مدمجة: الأساسية + الديناميكية من لوحة المالك (Firestore).
     * Eagerly يُبقي المستمع حياً حتى تظهر التصنيفات المنشورة فوراً دون انتظار فتح الشاشة.
     */
    // Core categories ship with the APK. Admin-panel edits do not mutate app content remotely.
    val categories: StateFlow<List<AzkarCategory>> =
        MutableStateFlow(CategoryDefaults.builtIn).asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    private var aiJob: Job? = null
    private val _lastAiLatencyMs = MutableStateFlow<Long?>(null)
    val lastAiLatencyMs: StateFlow<Long?> = _lastAiLatencyMs.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                "السلام عليكم، أنا المساعد المحلي. أبحث في الأذكار والمصادر المضمّنة على جهازك دون الاتصال بخدمة ذكاء اصطناعي سحابية.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // User account status is kept separate from the anonymous session used only for AI access.
    private val _userSignedIn = MutableStateFlow(authRepository.hasRegisteredAccount())
    val userSignedIn: StateFlow<Boolean> = _userSignedIn.asStateFlow()

    private val _aiAccessReady = MutableStateFlow(true)
    val aiAccessReady: StateFlow<Boolean> = _aiAccessReady.asStateFlow()

    private val _isPreparingAiAccess = MutableStateFlow(false)
    val isPreparingAiAccess: StateFlow<Boolean> = _isPreparingAiAccess.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    val myFeedback: StateFlow<List<FeedbackItem>> = firestoreRepository.observeMyFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadFeedbackCount: StateFlow<Int> = myFeedback
        .map { list -> list.count { it.replyUnread } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            progressRepository.initTodayProgress()
        }
    }

    fun addTasbeeh(count: Int = 1) {
        if (count <= 0) return
        viewModelScope.launch { progressRepository.addTasbeeh(count) }
    }

    fun flushPendingTasbeeh() = Unit

    fun completeSabah() {
        viewModelScope.launch { progressRepository.completeSabah() }
    }

    fun completeMasaa() {
        viewModelScope.launch { progressRepository.completeMasaa() }
    }

    fun completeSleep() {
        viewModelScope.launch { progressRepository.completeSleep() }
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
            firestoreRepository.updateNotificationPrefs(enabled)
        }
    }
    fun setReminderEnabled(category: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReminderEnabled(category, enabled) }
    }

    fun setReminderTime(category: String, time: String) {
        viewModelScope.launch { settingsRepository.setReminderTime(category, time) }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlay(enabled) }
    }
    fun setAudioSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setAudioSpeed(speed) }
    }

    fun setReciterId(id: String) {
        viewModelScope.launch { settingsRepository.setReciterId(id) }
    }

    fun setWeeklyDaysGoal(days: Int) {
        viewModelScope.launch { settingsRepository.setWeeklyGoal(days) }
    }

    fun setAccentTheme(key: String) {
        viewModelScope.launch { settingsRepository.setAccent(key) }
    }

    fun toggleFavorite(id: Int) {
        viewModelScope.launch { settingsRepository.toggleFavorite(id) }
    }
    fun saveAiZekr(text: String) {
        viewModelScope.launch {
            settingsRepository.saveAiZekr(text)
            _statusMessage.value = "تم حفظ إجابة المساعد في «أذكاري»."
        }
    }

    fun removeAiZekr(text: String) {
        viewModelScope.launch { settingsRepository.removeAiZekr(text) }
    }

    fun setReaderFont(key: String) {
        viewModelScope.launch { settingsRepository.setReaderFont(key) }
    }

    fun setDailyTasbeehGoal(goal: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(goal) }
    }

    fun setTasbihTarget(target: Int) {
        viewModelScope.launch { settingsRepository.setTasbihTarget(target) }
    }

    fun saveCustomWird(wird: CustomWird) {
        viewModelScope.launch { settingsRepository.saveCustomWird(wird) }
    }

    fun deleteCustomWird(id: String) {
        viewModelScope.launch { settingsRepository.deleteCustomWird(id) }
    }

    fun sendChatMessage(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _isLoadingAi.value) return
        _isLoadingAi.value = true
        _chatMessages.update {
            it + ChatMessage(message, isUser = true) +
                ChatMessage("جاري البحث...", isUser = false, isLoading = true)
        }
        aiJob = viewModelScope.launch {
            val startedAt = android.os.SystemClock.elapsedRealtime()
            try {
                val reply = try {
                    aiRepository.ask(message)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    "تعذر البحث في البيانات المحلية. أعد صياغة السؤال وحاول مرة أخرى."
                }
                _lastAiLatencyMs.value = android.os.SystemClock.elapsedRealtime() - startedAt
                _chatMessages.update { list ->
                    list.filterNot { it.isLoading } + ChatMessage(reply, isUser = false, actionsEnabled = true)
                }
            } finally {
                _isLoadingAi.value = false
                aiJob = null
            }
        }
    }

    fun cancelAiRequest() {
        if (!_isLoadingAi.value) return
        aiJob?.cancel()
        _chatMessages.update { list ->
            list.filterNot { it.isLoading } + ChatMessage("تم إلغاء الطلب.", isUser = false)
        }
        _isLoadingAi.value = false
        aiJob = null
    }

    fun clearStatusMessage() { _statusMessage.value = "" }

    /**
     * Opens the AI assistant without asking the user to create an account.
     * Firebase Anonymous Auth provides a server-side UID for rate limiting while
     * leaving the normal account/sync status unchanged.
     */
    /**
     * Firebase AI Logic authenticates the app with App Check; chat does not require
     * a Firebase Auth account or an anonymous-auth provider.
     */
    fun ensureAiAccess() {
        _aiAccessReady.value = true
        _isPreparingAiAccess.value = false
    }

    fun createAccountWithEmail(email: String, password: String) =
        runEmailAuthentication("إنشاء الحساب") { authRepository.createAccountWithEmail(email, password) }

    fun signInWithEmail(email: String, password: String) =
        runEmailAuthentication("تسجيل الدخول") { authRepository.signInWithEmail(email, password) }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _statusMessage.value = "أدخل بريدك الإلكتروني أولاً لإرسال رابط استعادة كلمة المرور."
            return
        }
        viewModelScope.launch {
            _isSigningIn.value = true
            try {
                authRepository.sendPasswordReset(email)
                _statusMessage.value = "إذا كان البريد مسجلاً، فسيصلك رابط استعادة كلمة المرور."
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _statusMessage.value = authErrorMessage(error, creating = false)
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    private fun runEmailAuthentication(action: String, operation: suspend () -> Unit) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _statusMessage.value = "جارٍ $action..."
            try {
                operation()
                _userSignedIn.value = authRepository.hasRegisteredAccount()
                _aiAccessReady.value = true
                _statusMessage.value = "تم $action بنجاح."
                runCatching {
                    val enabled = settingsRepository.notificationsEnabledFlow.first()
                    firestoreRepository.saveFcmToken(notificationsEnabled = enabled)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _statusMessage.value = authErrorMessage(error, creating = action == "إنشاء الحساب")
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    private fun authErrorMessage(error: Exception, creating: Boolean): String = when (error) {
        is FirebaseAuthWeakPasswordException -> "كلمة المرور ضعيفة. استخدم 6 أحرف على الأقل."
        is FirebaseAuthUserCollisionException -> "هذا البريد مسجّل مسبقاً. سجّل الدخول بدلاً من إنشاء حساب جديد."
        is FirebaseAuthInvalidUserException -> "لم يُعثر على حساب بهذا البريد. تحقق من البريد أو أنشئ حساباً."
        is FirebaseAuthInvalidCredentialsException -> if (creating) {
            "البريد الإلكتروني غير صالح أو تعذر إنشاء الحساب."
        } else {
            "البريد الإلكتروني أو كلمة المرور غير صحيحة."
        }
        else -> "تعذر إكمال العملية. تحقق من اتصال الإنترنت ومن تفعيل تسجيل الدخول بالبريد وكلمة المرور في Firebase."
    }

    fun signIn(context: Context) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _statusMessage.value = "جارٍ تسجيل الدخول باستخدام Google..."
            try {
                val success = authRepository.signInWithGoogle(context)
                if (success) {
                    _userSignedIn.value = authRepository.hasRegisteredAccount()
                    _aiAccessReady.value = true
                    _statusMessage.value = "تم تسجيل الدخول بنجاح!"
                    runCatching {
                        val enabled = settingsRepository.notificationsEnabledFlow.first()
                        firestoreRepository.saveFcmToken(notificationsEnabled = enabled)
                    }.onFailure {
                        // Authentication succeeded; notification token setup is a separate concern.
                        _statusMessage.value = "تم تسجيل الدخول، لكن تعذر تحديث رمز الإشعارات."
                    }
                } else {
                    _statusMessage.value = "لم يكتمل تسجيل الدخول. تحقق من إعداد Google في Firebase ثم أعد المحاولة."
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _statusMessage.value = "تعذر تسجيل الدخول. تحقق من اتصال الإنترنت وإعدادات Google ثم أعد المحاولة."
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _userSignedIn.value = false
            _aiAccessReady.value = true
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
                settingsRepository.clearAll()
                authRepository.signOut()
                _userSignedIn.value = false
                _aiAccessReady.value = true
                _statusMessage.value = "تم حذف الحساب نهائياً."
            } catch (e: FirebaseFunctionsException) {
                _statusMessage.value = when (e.code) {
                    FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                        "سجّل الدخول مجدداً ثم أعد المحاولة."
                    else -> "تعذر حذف الحساب. حاول لاحقاً (يمكنك إعادة المحاولة بأمان)."
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
            // Anonymous sessions are only for the AI assistant; do not treat them as
            // a registered account for personal feedback and support requests.
            if (!authRepository.hasRegisteredAccount()) {
                _statusMessage.value =
                    "لإرسال الطلبات ومتابعة الردود، أنشئ حساباً أو سجّل الدخول من الإعدادات."
                return@launch
            }
            val result = firestoreRepository.submitFeedback(draft, draft.attachmentUri)
            _statusMessage.value = if (result.isSuccess) {
                "تم إرسال طلبك بنجاح، ويمكنك متابعة حالته من هنا."
            } else {
                "تعذر إرسال الطلب. حاول مرة أخرى."
            }
        }
    }

    /**
     * يزامن **تقدم المستخدم** فقط (التسبيح والإنجاز اليومي) مع السحابة.
     * محتوى اللوحة (التصنيفات والأذكار المنشورة) يصل تلقائياً عبر مستمعي Firestore.
     */
    fun syncData() {
        viewModelScope.launch {
            if (authRepository.getCurrentUser() == null) {
                _statusMessage.value = "سجّل الدخول لمزامنة تقدمك. محتوى اللوحة يصل تلقائياً عند الاتصال."
                return@launch
            }
            _statusMessage.value = "جاري مزامنة التقدم مع السحابة..."
            val remote = firestoreRepository.fetchProgress()
            if (remote == null) {
                _statusMessage.value = "تعذرت مزامنة التقدم، تأكد من اتصالك وتسجيل الدخول 🌐"
                return@launch
            }
            progressRepository.syncProgress(remote)
            val local = progressRepository.getAllProgress().ifEmpty {
                progressRepository.getRecentProgress().first()
            }
            val ok = firestoreRepository.backupProgress(local)
            _statusMessage.value =
                if (ok) "تمت مزامنة التقدم. الأذكار المنشورة من اللوحة تُحدَّث تلقائياً عند الاتصال."
                else "تعذر رفع التقدم إلى السحابة"
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
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
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
