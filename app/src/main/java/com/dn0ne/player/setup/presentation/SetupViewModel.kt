package com.dn0ne.player.setup.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dn0ne.player.setup.data.SetupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SetupViewModel(
    private val setupState: SetupState,
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

    fun onFinishSetupClick() {
        setupState.isComplete = true
    }

    fun onAudioPermissionRequest(isGranted: Boolean) {
        _isAudioPermissionGranted.update {
            isGranted
        }
    }
}