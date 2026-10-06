package com.sipun.sonora.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
}

class ThemePreferences private constructor(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    private fun readTheme(): AppTheme =
        runCatching {
            AppTheme.valueOf(
                preferences.getString(KEY_THEME, AppTheme.SYSTEM.name)
                    ?: AppTheme.SYSTEM.name
            )
        }.getOrDefault(AppTheme.SYSTEM)

    companion object {
        private const val PREFERENCES_NAME = "sonora_preferences"
        private const val KEY_THEME = "app_theme"

        @Volatile
        private var instance: ThemePreferences? = null

        fun from(context: Context): ThemePreferences =
            instance ?: synchronized(this) {
                instance ?: ThemePreferences(context.applicationContext).also {
                    instance = it
                }
            }
    }
}
