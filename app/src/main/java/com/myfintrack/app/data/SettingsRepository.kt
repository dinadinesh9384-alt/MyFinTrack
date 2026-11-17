package com.myfintrack.app.data

import android.content.Context
import com.myfintrack.app.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists user preferences in private SharedPreferences (framework API, no extra dependency). */
class SettingsRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setHideBalances(hidden: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_BALANCES, hidden).apply()
        _settings.value = read()
    }

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim().take(MAX_NAME_LENGTH)).apply()
        _settings.value = read()
    }

    fun clear() {
        prefs.edit().clear().apply()
        _settings.value = read()
    }

    private fun read() = AppSettings(
        hideBalances = prefs.getBoolean(KEY_HIDE_BALANCES, false),
        displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
    )

    companion object {
        const val MAX_NAME_LENGTH = 30
        private const val PREFS_NAME = "myfintrack_settings"
        private const val KEY_HIDE_BALANCES = "hide_balances"
        private const val KEY_DISPLAY_NAME = "display_name"

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
