package com.dn0ne.player.app.presentation

import com.dn0ne.player.app.domain.metadata.Metadata
import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.domain.track.Track

sealed interface PlayerScreenEvent {
    data class OnTrackClick(val track: Track, val playlist: List<Track>): PlayerScreenEvent

    data object OnPlayClick: PlayerScreenEvent
    data object OnPauseClick: PlayerScreenEvent
    data object OnSeekToNextClick: PlayerScreenEvent
    data object OnSeekToPreviousClick: PlayerScreenEvent
    data class OnSeekTo(val position: Long): PlayerScreenEvent

    data object OnPlaybackModeClick: PlayerScreenEvent
    data object OnLyricsClick: PlayerScreenEvent

    data class OnPlayNextClick(val track: Track): PlayerScreenEvent
    data class OnAddToQueueClick(val track: Track): PlayerScreenEvent
    data class OnViewTrackInfoClick(val track: Track): PlayerScreenEvent
    data object OnCloseTrackInfoSheetClick: PlayerScreenEvent
    data object OnAcceptingRisksOfMetadataEditing: PlayerScreenEvent
    data class OnSearchInfo(val query: String): PlayerScreenEvent
    data object OnMatchDurationWhenSearchMetadataClick: PlayerScreenEvent
    data class OnMetadataSearchResultPick(val searchResult: MetadataSearchResult): PlayerScreenEvent
    data class OnOverwriteMetadataClick(val metadata: Metadata): PlayerScreenEvent
    data object OnRestoreCoverArtClick: PlayerScreenEvent
    data class OnConfirmMetadataEditClick(val metadata: Metadata): PlayerScreenEvent
}