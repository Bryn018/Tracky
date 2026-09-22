package com.tracky.app.ui.screens.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean> = settingsRepository.isFirstLaunch()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setFirstLaunch(false)
        }
    }
}
