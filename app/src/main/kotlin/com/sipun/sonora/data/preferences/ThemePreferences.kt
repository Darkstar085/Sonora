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

enum class DynamicPalette {
    TONAL_SPOT,
    VIBRANT,
    EXPRESSIVE,
    NEUTRAL,
    MONOCHROME,
}

class ThemePreferences private constructor(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _pureBlack = MutableStateFlow(readPureBlack())
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _dynamicColor = MutableStateFlow(readDynamicColor())
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _dynamicPalette = MutableStateFlow(readDynamicPalette())
    val dynamicPalette: StateFlow<DynamicPalette> = _dynamicPalette.asStateFlow()

    private val _customColor = MutableStateFlow(readCustomColor())
    val customColor: StateFlow<Int> = _customColor.asStateFlow()

    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    fun setPureBlack(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PURE_BLACK, enabled).apply()
        _pureBlack.value = enabled
    }

    fun setDynamicColor(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setDynamicPalette(palette: DynamicPalette) {
        preferences.edit().putString(KEY_DYNAMIC_PALETTE, palette.name).apply()
        _dynamicPalette.value = palette
    }

    fun setCustomColor(color: Int) {
        preferences.edit().putInt(KEY_CUSTOM_COLOR, color).apply()
        _customColor.value = color
    }

    private fun readPureBlack(): Boolean =
        preferences.getBoolean(KEY_PURE_BLACK, false)

    private fun readDynamicColor(): Boolean =
        preferences.getBoolean(KEY_DYNAMIC_COLOR, false)

    private fun readCustomColor(): Int = preferences.getInt(KEY_CUSTOM_COLOR, 0xFFE92B2B.toInt())

    private fun readDynamicPalette(): DynamicPalette =
        runCatching {
            DynamicPalette.valueOf(
                preferences.getString(KEY_DYNAMIC_PALETTE, DynamicPalette.TONAL_SPOT.name)
                    ?: DynamicPalette.TONAL_SPOT.name
            )
        }.getOrDefault(DynamicPalette.TONAL_SPOT)

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
        private const val KEY_PURE_BLACK = "pure_black"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_DYNAMIC_PALETTE = "dynamic_palette"
        private const val KEY_CUSTOM_COLOR = "custom_color"

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
