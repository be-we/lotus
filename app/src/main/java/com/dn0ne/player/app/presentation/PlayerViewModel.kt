package com.dn0ne.player.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.data.TrackResolver
import com.dn0ne.player.app.domain.playback.PlaybackMode
import com.dn0ne.player.app.presentation.components.playback.PlaybackState
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.trackinfo.TrackInfoSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val savedPlayerState: SavedPlayerState,
    private val trackResolver: TrackResolver
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

    private var positionUpdateJob: Job? = null

    private val _trackInfoSheetState = MutableStateFlow(TrackInfoSheetState())
    val trackInfoSheetState = _trackInfoSheetState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TrackInfoSheetState()
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val tracks = trackResolver.getTracks()
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
                        positionUpdateJob = startPositionUpdate()
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

                        positionUpdateJob?.cancel()
                        if (isPlaying) {
                            positionUpdateJob = startPositionUpdate()
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

                        positionUpdateJob?.cancel()
                        positionUpdateJob = startPositionUpdate()
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
                }
            }

            is PlayerScreenEvent.OnPlayNextClick -> {
                if (_playbackState.value.playlist == null || _playbackState.value.playlist?.isEmpty() == true) {
                    _playbackState.update {
                        it.copy(
                            playlist = listOf(event.track),
                            currentTrack = event.track
                        )
                    }

                    player?.run {
                        addMediaItem(event.track.mediaItem)
                        prepare()
                        play()
                    }
                } else {
                    _playbackState.value.playlist?.let { playlist ->
                        if (!playlist.contains(event.track)){
                            _playbackState.update {
                                it.copy(
                                    playlist = playlist.toMutableList() + event.track
                                )
                            }
                        }
                    }

                    player?.run {
                        addMediaItem(currentMediaItemIndex + 1, event.track.mediaItem)
                    }
                }
            }
            is PlayerScreenEvent.OnAddToQueueClick -> {
                if (_playbackState.value.playlist == null || _playbackState.value.playlist?.isEmpty() == true) {
                    _playbackState.update {
                        it.copy(
                            playlist = listOf(event.track),
                            currentTrack = event.track
                        )
                    }

                    player?.run {
                        addMediaItem(event.track.mediaItem)
                        prepare()
                        play()
                    }
                } else {
                    _playbackState.value.playlist?.let { playlist ->
                        if (!playlist.contains(event.track)){
                            _playbackState.update {
                                it.copy(
                                    playlist = playlist.toMutableList() + event.track
                                )
                            }
                        }
                    }

                    player?.run {
                        addMediaItem(event.track.mediaItem)
                    }
                }
            }

            is PlayerScreenEvent.OnViewTrackInfoClick -> {
                _trackInfoSheetState.update {
                    it.copy(
                        isShown = true,
                        track = event.track
                    )
                }
            }

            PlayerScreenEvent.OnCloseTrackInfoSheetClick -> {
                _trackInfoSheetState.update {
                    it.copy(
                        isShown = false
                    )
                }
            }
        }
    }

    private fun startPositionUpdate(): Job {
        return viewModelScope.launch {
            player?.let { player ->
                while (_playbackState.value.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            position = player.currentPosition
                        )
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun setPlayerPlaybackMode(playbackMode: PlaybackMode) {
        when (playbackMode) {
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