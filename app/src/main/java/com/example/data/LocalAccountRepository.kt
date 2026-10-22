package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * One offline account per app installation. Only a salted password hash is stored;
 * passwords are never persisted. Account identity/session is device-local and cannot
 * be used to authenticate with Firebase or synchronize across devices.
 */
class LocalAccountRepository(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val accountExists: Boolean
        get() = !preferences.getString(KEY_USERNAME, null).isNullOrBlank()

    val currentUsername: String?
        get() = preferences.getString(KEY_SESSION_USERNAME, null)

    suspend fun createAccount(username: String, password: String) = withContext(Dispatchers.IO) {
        val cleanUsername = validateUsername(username)
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "يجب أن تتكون كلمة المرور من $MIN_PASSWORD_LENGTH أحرف على الأقل."
        }
        check(!accountExists) {
            "يوجد حساب محلي على هذا الجهاز بالفعل. سجّل الدخول به بدلاً من إنشاء حساب آخر."
        }

        val salt = ByteArray(SALT_SIZE).also(secureRandom::nextBytes)
        val hash = derivePasswordHash(password, salt)
        val saved = preferences.edit()
            .putString(KEY_USERNAME, cleanUsername)
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_SESSION_USERNAME, cleanUsername)
            .commit()
        check(saved) { "تعذر حفظ الحساب على هذا الجهاز. حاول مرة أخرى." }
    }

    suspend fun signIn(username: String, password: String) = withContext(Dispatchers.IO) {
        val savedUsername = preferences.getString(KEY_USERNAME, null)
            ?: error("لا يوجد حساب محلي بعد. أنشئ حساباً أولاً.")
        val cleanUsername = validateUsername(username)
        require(cleanUsername.lowercase(Locale.ROOT) == savedUsername.lowercase(Locale.ROOT)) {
            "اسم المستخدم أو كلمة المرور غير صحيحة."
        }

        val salt = preferences.getString(KEY_SALT, null)
            ?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: error("بيانات الحساب غير مكتملة. أنشئ حساباً محلياً جديداً.")
        val expectedHash = preferences.getString(KEY_PASSWORD_HASH, null)
            ?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: error("بيانات الحساب غير مكتملة. أنشئ حساباً محلياً جديداً.")
        val actualHash = derivePasswordHash(password, salt)
        require(MessageDigest.isEqual(expectedHash, actualHash)) {
            "اسم المستخدم أو كلمة المرور غير صحيحة."
        }
        val saved = preferences.edit().putString(KEY_SESSION_USERNAME, savedUsername).commit()
        check(saved) { "تعذر حفظ جلسة الدخول. حاول مرة أخرى." }
    }

    fun signOut() {
        preferences.edit().remove(KEY_SESSION_USERNAME).apply()
    }

    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        val saved = preferences.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_SALT)
            .remove(KEY_PASSWORD_HASH)
            .remove(KEY_SESSION_USERNAME)
            .commit()
        check(saved) { "تعذر حذف الحساب المحلي." }
    }

    private fun validateUsername(username: String): String {
        val clean = username.trim()
        require(clean.matches(USERNAME_PATTERN)) {
            "اسم المستخدم يجب أن يكون من 3 إلى 32 حرفاً أو رقماً، ويمكن استخدام النقطة والشرطة والشرطة السفلية دون مسافات."
        }
        return clean
    }

    private fun derivePasswordHash(password: String, salt: ByteArray): ByteArray {
        val passwordChars = password.toCharArray()
        val spec = PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
            passwordChars.fill(0.toChar())
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "local_account"
        const val KEY_USERNAME = "username"
        const val KEY_SALT = "password_salt"
        const val KEY_PASSWORD_HASH = "password_hash"
        const val KEY_SESSION_USERNAME = "session_username"
        const val MIN_PASSWORD_LENGTH = 8
        const val SALT_SIZE = 16
        const val HASH_BITS = 256
        const val PBKDF2_ITERATIONS = 210_000
        val USERNAME_PATTERN = Regex("^[\\p{L}\\p{N}_.-]{3,32}$")
        val secureRandom = SecureRandom()
    }
}
