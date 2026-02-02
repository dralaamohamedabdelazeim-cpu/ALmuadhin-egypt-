package com.example.almuadhin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.almuadhin.data.AdhanSound
import com.example.almuadhin.data.CalculationMethod
import com.example.almuadhin.data.LocationMode
import com.example.almuadhin.data.SettingsRepository
import com.example.almuadhin.data.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UserSettings())
    val ui: StateFlow<UserSettings> = _ui

    init {
        viewModelScope.launch {
            repo.settingsFlow.collect { _ui.value = it }
        }
    }

    fun setLocationMode(mode: LocationMode) = viewModelScope.launch { repo.setLocationMode(mode) }
    fun setManual(city: String, country: String) = viewModelScope.launch { repo.setManualLocation(city, country) }
    fun setMethod(method: CalculationMethod) = viewModelScope.launch { repo.setCalculationMethod(method) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { repo.setNotificationsEnabled(enabled) }
    fun setAdsRemoved(removed: Boolean) = viewModelScope.launch { repo.setAdsRemoved(removed) }
    fun setAdCooldown(minutes: Int) = viewModelScope.launch { repo.setAdCooldownMinutes(minutes) }
    fun setAdhanSound(sound: AdhanSound) = viewModelScope.launch { repo.setAdhanSound(sound) }
    fun setPlayFullAdhan(playFull: Boolean) = viewModelScope.launch { repo.setPlayFullAdhan(playFull) }

    suspend fun current(): UserSettings = repo.settingsFlow.first()
}
