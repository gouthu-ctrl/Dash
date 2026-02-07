package com.dash.travel.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    private val _themeFlow = MutableStateFlow(prefs.getString("theme_preference", "system") ?: "system")
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _languageFlow = MutableStateFlow(prefs.getString("language_preference", "en") ?: "en")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    fun setTheme(theme: String) {
        prefs.edit().putString("theme_preference", theme).apply()
        _themeFlow.value = theme
    }

    fun setLanguage(language: String) {
        prefs.edit().putString("language_preference", language).apply()
        _languageFlow.value = language
    }
}
