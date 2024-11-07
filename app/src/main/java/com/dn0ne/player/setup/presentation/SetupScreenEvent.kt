package com.dn0ne.player.setup.presentation

import com.dn0ne.player.core.data.LyricsProvider
import com.dn0ne.player.core.data.MetadataProvider

sealed interface SetupScreenEvent {
    data class OnMetadataProviderClick(val provider: MetadataProvider): SetupScreenEvent
    data class OnLyricsProviderClick(val provider: LyricsProvider): SetupScreenEvent
    data object OnFinishSetupClick: SetupScreenEvent
}