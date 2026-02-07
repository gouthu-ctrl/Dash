package com.dash.travel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.repository.ProfileRepository
import com.dash.travel.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: String = "system",
    val language: String = "en",
    val isLoading: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository? = null,
    private val userId: String? = null
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeFlow,
        settingsRepository.languageFlow
    ) { theme, language ->
        SettingsUiState(theme, language)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
            // Sync with Supabase if logged in
             userId?.let { uid ->
                 // TODO: Update Profile in Supabase
                 // This requires ProfileRepository to have an update function or direct Supabase call
                 // For now, we focus on local persistence which is critical for UI updates
             }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
             userId?.let { uid ->
                 // Todo sync
             }
        }
    }
}
