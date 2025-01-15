package com.dn0ne.player.app.presentation.components.settings

import com.dn0ne.player.EqualizerController
import com.dn0ne.player.core.data.Settings

data class SettingsSheetState(
    val settings: Settings,
    val isShown: Boolean = false,
    val equalizerController: EqualizerController,
    val foldersWithAudio: Set<String> = emptySet()
)
