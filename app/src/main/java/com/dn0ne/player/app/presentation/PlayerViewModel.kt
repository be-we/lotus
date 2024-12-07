package com.dn0ne.player.app.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.dn0ne.player.R
import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.data.remote.lyrics.LyricsProvider
import com.dn0ne.player.app.data.remote.metadata.MetadataProvider
import com.dn0ne.player.app.data.repository.LyricsRepository
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.metadata.Metadata
import com.dn0ne.player.app.domain.playback.PlaybackMode
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.playback.PlaybackState
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarController
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarEvent
import com.dn0ne.player.app.presentation.components.trackinfo.ChangesSheetState
import com.dn0ne.player.app.presentation.components.trackinfo.InfoSearchSheetState
import com.dn0ne.player.app.presentation.components.trackinfo.ManualInfoEditSheetState
import com.dn0ne.player.app.presentation.components.trackinfo.TrackInfoSheetState
import com.dn0ne.player.core.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(
    private val savedPlayerState: SavedPlayerState,
    private val trackRepository: TrackRepository,
    private val metadataProvider: MetadataProvider,
    private val lyricsProvider: LyricsProvider,
    private val lyricsRepository: LyricsRepository,
    private val settings: Settings
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

    private val _infoSearchSheetState = MutableStateFlow(InfoSearchSheetState())
    private val _changesSheetState = MutableStateFlow(ChangesSheetState())
    private val _manualInfoEditSheetState = MutableStateFlow(ManualInfoEditSheetState())
    private val _trackInfoSheetState = MutableStateFlow(
        TrackInfoSheetState(
            showRisksOfMetadataEditingDialog = !settings.areRisksOfMetadataEditingAccepted
        )
    )
    val trackInfoSheetState = combine(
        _trackInfoSheetState, _infoSearchSheetState, _changesSheetState, _manualInfoEditSheetState
    ) { trackInfoSheetState, infoSearchSheetState, changesSheetState, manualInfoEditSheetState ->
        trackInfoSheetState.copy(
            infoSearchSheetState = infoSearchSheetState,
            changesSheetState = changesSheetState,
            manualInfoEditSheetState = manualInfoEditSheetState
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = TrackInfoSheetState()
        )

    private val _pendingMetadata = Channel<Pair<Track, Metadata>>()
    val pendingMetadata = _pendingMetadata.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val tracks = trackRepository.getTracks()
                if (_trackList.value != tracks) {
                    _trackList.update {
                        tracks
                    }

                    if (_trackInfoSheetState.value.track != null) {
                        _trackInfoSheetState.update {
                            it.copy(
                                track = _trackList.value.find { track -> it.track?.uri == track.uri }
                            )
                        }

                        _playbackState.update {
                            PlaybackState()
                        }

                        withContext(Dispatchers.Main) {
                            player?.stop()
                            player?.clearMediaItems()
                        }
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

            PlayerScreenEvent.OnLyricsClick -> {
                _playbackState.value.currentTrack?.let { currentTrack ->
                    if (currentTrack.uri.toString() == _playbackState.value.lyrics?.uri) return

                    _playbackState.update {
                        it.copy(
                            lyrics = null,
                            isLoadingLyrics = true
                        )
                    }

                    var lyrics: Lyrics? = lyricsRepository.getLyricsByUri(currentTrack.uri.toString())

                    if (lyrics == null) {
                        if (currentTrack.title == null || currentTrack.artist == null) {
                            viewModelScope.launch {
                                SnackbarController.sendEvent(
                                    SnackbarEvent(
                                        message = R.string.cant_look_for_lyrics_title_or_artist_is_missing
                                    )
                                )
                            }
                            return
                        }

                        viewModelScope.launch {
                            val result = lyricsProvider.getLyrics(currentTrack)

                            when(result) {
                                is Result.Success -> {
                                    lyrics = result.data

                                    lyricsRepository.insertLyrics(lyrics)

                                    _playbackState.update {
                                        it.copy(
                                            lyrics = lyrics,
                                            isLoadingLyrics = false
                                        )
                                    }
                                }
                                is Result.Error -> {
                                    _playbackState.update {
                                        it.copy(
                                            isLoadingLyrics = false
                                        )
                                    }
                                    when(result.error) {
                                        DataError.Network.BadRequest -> {
                                            SnackbarController.sendEvent(
                                                SnackbarEvent(
                                                    message = R.string.cant_look_for_lyrics_title_or_artist_is_missing
                                                )
                                            )
                                        }
                                        DataError.Network.NotFound -> {
                                            SnackbarController.sendEvent(
                                                SnackbarEvent(
                                                    message = R.string.lyrics_not_found
                                                )
                                            )
                                        }
                                        DataError.Network.ParseError -> {
                                            SnackbarController.sendEvent(
                                                SnackbarEvent(
                                                    message = R.string.failed_to_parse_response
                                                )
                                            )
                                        }
                                        DataError.Network.NoInternet -> {
                                            SnackbarController.sendEvent(
                                                SnackbarEvent(
                                                    message = R.string.no_internet
                                                )
                                            )
                                        }
                                        else -> {
                                            SnackbarController.sendEvent(
                                                SnackbarEvent(
                                                    message = R.string.unknown_error_occurred
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        _playbackState.update {
                            it.copy(
                                lyrics = lyrics,
                                isLoadingLyrics = false
                            )
                        }
                    }
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
                        if (!playlist.contains(event.track)) {
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
                        if (!playlist.contains(event.track)) {
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

            PlayerScreenEvent.OnAcceptingRisksOfMetadataEditing -> {
                settings.areRisksOfMetadataEditingAccepted = true
                _trackInfoSheetState.update {
                    it.copy(
                        showRisksOfMetadataEditingDialog = false
                    )
                }
            }

            PlayerScreenEvent.OnMatchDurationWhenSearchMetadataClick -> {
                settings.matchDurationWhenSearchMetadata = !settings.matchDurationWhenSearchMetadata
            }

            is PlayerScreenEvent.OnSearchInfo -> {
                viewModelScope.launch {
                    _infoSearchSheetState.update {
                        it.copy(
                            isLoading = true
                        )
                    }

                    val result = metadataProvider.searchMetadata(
                        query = event.query,
                        trackDuration = _trackInfoSheetState.value.track?.duration?.toLong() ?: return@launch
                    )
                    when (result) {
                        is Result.Error -> {
                            when (result.error) {
                                DataError.Network.BadRequest -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.query_was_corrupted
                                        )
                                    )
                                    Log.d("Metadata Search", "${result.error} - ${event.query}")
                                }

                                DataError.Network.InternalServerError -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.musicbrainz_server_error
                                        )
                                    )
                                }

                                DataError.Network.ServiceUnavailable -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.musicbrainz_is_unavailable
                                        )
                                    )
                                }

                                DataError.Network.ParseError -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.failed_to_parse_response
                                        )
                                    )
                                    Log.d("Metadata Search", "${result.error} - ${event.query}")
                                }

                                DataError.Network.NoInternet -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.no_internet
                                        )
                                    )
                                }

                                else -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.unknown_error_occurred
                                        )
                                    )
                                    Log.d("Metadata Search", "${result.error} - ${event.query}")
                                }
                            }
                        }

                        is Result.Success -> {
                            _infoSearchSheetState.update {
                                it.copy(
                                    searchResults = result.data
                                )
                            }
                        }
                    }

                    _infoSearchSheetState.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                }
            }

            is PlayerScreenEvent.OnMetadataSearchResultPick -> {
                viewModelScope.launch {
                    _changesSheetState.update {
                        it.copy(
                            isLoadingArt = true
                        )
                    }
                    val result = metadataProvider.getCoverArtBytes(event.searchResult)
                    var coverArtBytes: ByteArray? = null
                    when (result) {
                        is Result.Success -> {
                            coverArtBytes = result.data
                            _changesSheetState.update {
                                it.copy(
                                    isLoadingArt = false,
                                    metadata = it.metadata.copy(
                                        coverArtBytes = coverArtBytes
                                    )
                                )
                            }
                        }

                        is Result.Error -> {
                            when (result.error) {
                                DataError.Network.BadRequest -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.failed_to_load_cover_art_album_id_corrupted,
                                        )
                                    )
                                }

                                DataError.Network.NotFound -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.cover_art_not_found
                                        )
                                    )
                                }

                                DataError.Network.ServiceUnavailable -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.cover_art_archive_is_unavailable
                                        )
                                    )
                                }

                                DataError.Network.NoInternet -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.no_internet
                                        )
                                    )
                                }

                                DataError.Network.RequestTimeout -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.failed_to_load_cover_art_request_timeout
                                        )
                                    )
                                }

                                else -> {
                                    SnackbarController.sendEvent(
                                        SnackbarEvent(
                                            message = R.string.unknown_error_occurred
                                        )
                                    )
                                }
                            }
                            _changesSheetState.update {
                                it.copy(
                                    isLoadingArt = false
                                )
                            }
                            return@launch
                        }
                    }
                }

                _changesSheetState.update {
                    it.copy(
                        metadata = Metadata(
                            title = event.searchResult.title,
                            album = event.searchResult.album,
                            artist = event.searchResult.artist,
                            albumArtist = event.searchResult.albumArtist,
                            genre = event.searchResult.genres?.joinToString(" / "),
                            year = event.searchResult.year,
                            trackNumber = event.searchResult.trackNumber
                        ),
                        isArtFromGallery = false
                    )
                }
            }

            is PlayerScreenEvent.OnOverwriteMetadataClick -> {
                viewModelScope.launch {
                    _trackInfoSheetState.value.track?.let { track ->
                        _pendingMetadata.send(track to event.metadata)
                    }
                }
            }

            PlayerScreenEvent.OnRestoreCoverArtClick -> {
                _manualInfoEditSheetState.update {
                    it.copy(
                        pickedCoverArtBytes = null
                    )
                }
            }

            is PlayerScreenEvent.OnConfirmMetadataEditClick -> {
                _changesSheetState.update {
                    it.copy(
                        metadata = event.metadata,
                        isArtFromGallery = event.metadata.coverArtBytes != null
                    )
                }
            }
        }
    }

    fun setPickedCoverArtBytes(bytes: ByteArray) {
        _manualInfoEditSheetState.update {
            it.copy(
                pickedCoverArtBytes = bytes
            )
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
                    delay(50)
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