package com.dn0ne.player.setup.presentation

sealed interface SetupScreenEvent {
    data object OnNextClick: SetupScreenEvent
    data class OnGrantAudioPermissionClick(val isGranted: Boolean): SetupScreenEvent
    data object OnFinishClick: SetupScreenEvent
}