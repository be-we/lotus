package com.dn0ne.player.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.dn0ne.player.app.data.MusicResolver
import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.domain.playback.PlaybackMode
import com.dn0ne.player.app.domain.playback.PlaybackState
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val savedPlayerState: SavedPlayerState,
    private val musicResolver: MusicResolver
) : ViewModel() {
    var player: Player? = null

    private val _trackList = MutableStateFlow(emptyList<Track>())
    val trackList = _trackList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PlaybackState()
        )

    init {
        viewModelScope.launch {
            while (true) {
                val tracks = musicResolver.getTracks()
                if (_trackList.value != tracks) {
                    _trackList.update {
                        tracks
                    }
                }
                delay(5000L)
            }
        }

        viewModelScope.launch {
            while (player == null) delay(500L)

            if (player?.currentMediaItem != null) {
                val playlist = savedPlayerState.playlist
                playlist?.let { playlist ->
                    _playbackState.update {
                        it.copy(
                            playlist = playlist,
                            currentTrack = playlist.find { player!!.currentMediaItem == it.mediaItem },
                            isPlaying = player!!.isPlaying,
                            position = player!!.currentPosition
                        )
                    }

                    if (player!!.isPlaying) {
                        startPositionUpdate()
                    }
                }
            }

            val playbackMode = savedPlayerState.playbackMode
            setPlayerPlaybackMode(playbackMode)
            _playbackState.update {
                it.copy(
                    playbackMode = playbackMode
                )
            }

            player?.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)

                        _playbackState.update {
                            it.copy(
                                isPlaying = isPlaying
                            )
                        }

                        if (isPlaying) {
                            startPositionUpdate()
                        }
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int
                    ) {
                        super.onMediaItemTransition(mediaItem, reason)

                        _playbackState.update {
                            it.copy(
                                currentTrack = it.playlist?.find {
                                    it.mediaItem == mediaItem
                                },
                                isPlaying = true,
                                position = 0L
                            )
                        }
                        startPositionUpdate()
                    }
                }
            )

        }
    }

    fun onEvent(event: PlayerScreenEvent) {

        when (event) {
            is PlayerScreenEvent.OnTrackClick -> {
                player?.let { player ->
                    if (_playbackState.value.playlist != event.playlist) {
                        player.clearMediaItems()
                        player.addMediaItems(
                            event.playlist.map { track -> track.mediaItem }
                        )
                    }

                    _playbackState.update {
                        it.copy(
                            playlist = event.playlist,
                            currentTrack = event.track,
                        )
                    }
                    savedPlayerState.playlist = event.playlist

                    player.seekTo(
                        _playbackState.value.playlist!!.indexOfFirst { it == event.track },
                        0L
                    )
                    player.prepare()
                    player.play()

                    _playbackState.update {
                        it.copy(
                            position = 0L
                        )
                    }

                }
            }

            PlayerScreenEvent.OnPauseClick -> {
                player?.run {
                    pause()
                }
            }

            PlayerScreenEvent.OnPlayClick -> {
                player?.let { player ->
                    if (player.currentMediaItem == null) return

                    player.prepare()
                    player.play()
                }
            }

            PlayerScreenEvent.OnSeekToNextClick -> {
                player?.let { player ->
                    if (!player.hasNextMediaItem()) return

                    player.seekToNextMediaItem()

                    player.prepare()
                    player.play()
                }
            }

            PlayerScreenEvent.OnSeekToPreviousClick -> {
                player?.let { player ->
                    if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()

                        player.prepare()
                        player.play()
                    } else {
                        player.seekTo(0L)
                        _playbackState.update {
                            it.copy(
                                position = 0L
                            )
                        }
                    }
                }
            }

            is PlayerScreenEvent.OnSeekTo -> {
                player?.let { player ->
                    if (player.currentMediaItem == null) return

                    player.seekTo(event.position)
                    _playbackState.update {
                        it.copy(
                            position = event.position
                        )
                    }

                    startPositionUpdate()
                }
            }

            PlayerScreenEvent.OnPlaybackModeClick -> {
                val newPlaybackMode = _playbackState.value.playbackMode.let {
                    PlaybackMode.entries.nextAfterOrNull(it.ordinal)
                }
                newPlaybackMode?.let { mode ->
                    setPlayerPlaybackMode(mode)
                    _playbackState.update {
                        it.copy(
                            playbackMode = mode
                        )
                    }
                    savedPlayerState.playbackMode = mode

                    if (_playbackState.value.isPlaying)
                        startPositionUpdate()
                }
            }
        }
    }

    private fun startPositionUpdate() {
        viewModelScope.launch {
            player?.let { player ->
                var playbackState = _playbackState.value
                while (_playbackState.value == playbackState && playbackState.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            position = player.currentPosition
                        )
                    }
                    playbackState = _playbackState.value
                    delay(1000)
                }
            }
        }
    }

    private fun setPlayerPlaybackMode(playbackMode: PlaybackMode) {
        when(playbackMode) {
            PlaybackMode.Repeat -> {
                player?.repeatMode = Player.REPEAT_MODE_ALL
                player?.shuffleModeEnabled = false
            }
            PlaybackMode.RepeatOne -> {
                player?.repeatMode = Player.REPEAT_MODE_ONE
                player?.shuffleModeEnabled = false
            }
            PlaybackMode.Shuffle -> {
                player?.repeatMode = Player.REPEAT_MODE_ALL
                player?.shuffleModeEnabled = true
            }
        }
    }

    /**
     * Returns next element after [index]. If next element index is out of bounds returns first element.
     * If index is negative returns `null`
     */
    private fun <T> List<T>.nextAfterOrNull(index: Int): T? {
        return getOrNull((index + 1) % size)
    }
}