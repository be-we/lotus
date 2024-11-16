package com.dn0ne.player.app.presentation

import com.dn0ne.player.app.domain.track.Track

sealed interface PlayerScreenEvent {
    data class OnTrackClick(val track: Track, val playlist: List<Track>): PlayerScreenEvent
    data object OnPlayClick: PlayerScreenEvent
    data object OnPauseClick: PlayerScreenEvent
    data object OnSeekToNextClick: PlayerScreenEvent
    data object OnSeekToPreviousClick: PlayerScreenEvent
    data class OnSeekTo(val position: Long): PlayerScreenEvent
    data object OnPlaybackModeClick: PlayerScreenEvent
}