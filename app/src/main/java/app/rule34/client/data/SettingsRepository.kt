package app.rule34.client.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class UserSettings(
    val ageConfirmed: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val hasCredentials: Boolean = false,
)

class SettingsRepository(
    private val context: Context,
    credentials: CredentialsRepository,
) {
    val state: Flow<UserSettings> = combine(
        context.settingsDataStore.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        },
        credentials.credentials,
    ) { prefs, creds ->
        UserSettings(
            ageConfirmed = prefs[Keys.AGE] ?: false,
            theme = ThemeMode.fromStored(prefs[Keys.THEME]),
            dynamicColor = prefs[Keys.DYNAMIC] ?: true,
            hasCredentials = creds != null,
        )
    }

    suspend fun confirmAge() {
        context.settingsDataStore.edit { it[Keys.AGE] = true }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC] = enabled }
    }

    private object Keys {
        val AGE = booleanPreferencesKey("age_confirmed")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
    }
}
