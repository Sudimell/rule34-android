package app.rule34.client.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CredentialsRepository(context: Context) {
    private val prefs = openPrefs(context)
    private val state = MutableStateFlow(read())
    val credentials: StateFlow<Credentials?> = state

    fun save(userId: String, apiKey: String) {
        prefs.edit()
            .putString(KEY_USER, userId.trim())
            .putString(KEY_API, apiKey.trim())
            .apply()
        state.value = read()
    }

    fun clear() {
        prefs.edit().clear().apply()
        state.value = null
    }

    private fun read(): Credentials? {
        val userId = prefs.getString(KEY_USER, null)?.trim().orEmpty()
        val apiKey = prefs.getString(KEY_API, null)?.trim().orEmpty()
        if (userId.isEmpty() || apiKey.isEmpty()) return null
        return Credentials(userId, apiKey)
    }

    private fun openPrefs(context: Context): SharedPreferences =
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FALLBACK_NAME, Context.MODE_PRIVATE)
        }

    private companion object {
        const val PREFS_NAME = "rule34_credentials"
        const val FALLBACK_NAME = "rule34_credentials_fallback"
        const val KEY_USER = "user_id"
        const val KEY_API = "api_key"
    }
}
