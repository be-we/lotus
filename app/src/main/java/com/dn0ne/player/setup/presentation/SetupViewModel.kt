package com.dn0ne.player.setup.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dn0ne.player.core.data.Settings
import com.dn0ne.player.setup.data.SetupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SetupViewModel(
    private val setupState: SetupState,
    private val settings: Settings,
) : ViewModel() {
    val startDestination: SetupPage by mutableStateOf(
        if (!setupState.isComplete) {
            SetupPage.Welcome
        } else SetupPage.AudioPermission
    )

    private var _isAudioPermissionGranted = MutableStateFlow(false)
    val isAudioPermissionGranted = _isAudioPermissionGranted.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = false
    )

    private var _selectedMetadataProvider = MutableStateFlow(settings.metadataProvider)
    val selectedMetadataProvider = _selectedMetadataProvider.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = settings.metadataProvider
    )

    private var _selectedLyricsProvider = MutableStateFlow(settings.lyricsProvider)
    val selectedLyricsProvider = _selectedLyricsProvider.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = settings.lyricsProvider
    )

    fun onEvent(event: SetupScreenEvent) {
        when (event) {
            is SetupScreenEvent.OnLyricsProviderClick -> {
                settings.lyricsProvider = event.provider
                _selectedLyricsProvider.update {
                    event.provider
                }
            }
            is SetupScreenEvent.OnMetadataProviderClick -> {
                settings.metadataProvider = event.provider
                _selectedMetadataProvider.update {
                    event.provider
                }
            }

            SetupScreenEvent.OnFinishSetupClick -> {
                setupState.isComplete = true
            }
        }
    }

    fun onAudioPermissionRequest(isGranted: Boolean) {
        _isAudioPermissionGranted.update {
            isGranted
        }
    }
}