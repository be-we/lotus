package com.dn0ne.player.setup.presentation

import androidx.lifecycle.ViewModel
import com.dn0ne.player.core.data.Settings
import com.dn0ne.player.setup.data.SetupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SetupViewModel(
    private val setupState: SetupState,
    private val settings: Settings,
) : ViewModel() {
    private val _currentPage = MutableStateFlow(
        if (!setupState.isComplete) {
            SetupPage.Welcome
        } else SetupPage.AudioPermission
    )
    val currentPage = _currentPage.asStateFlow()

    fun onEvent(event: SetupScreenEvent) {
        when(event) {
            SetupScreenEvent.OnNextClick -> {
                when(_currentPage.value) {
                    SetupPage.Welcome -> {
                        _currentPage.update {
                            SetupPage.AudioPermission
                        }
                    }
                    SetupPage.LyricsProvider -> {
                        _currentPage.update {
                            SetupPage.MetadataProvider
                        }
                    }
                    else -> Unit
                }
            }
            is SetupScreenEvent.OnGrantAudioPermissionClick -> TODO()
            SetupScreenEvent.OnFinishClick -> TODO()
        }

    }
}