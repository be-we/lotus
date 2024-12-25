package com.dn0ne.player.app.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.TrackSort
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.filterTracks
import com.dn0ne.player.app.presentation.components.topbar.LazyGridWithCollapsibleTabsTopBar
import com.dn0ne.player.app.presentation.components.PlaylistSortButton
import com.dn0ne.player.app.presentation.components.TrackSortButton
import com.dn0ne.player.app.presentation.components.playback.PlayerSheet
import com.dn0ne.player.app.presentation.components.playlist.AddToOrCreatePlaylistBottomSheet
import com.dn0ne.player.app.presentation.components.playlist.DeletePlaylistDialog
import com.dn0ne.player.app.presentation.components.playlist.MutablePlaylist
import com.dn0ne.player.app.presentation.components.playlist.Playlist
import com.dn0ne.player.app.presentation.components.playlist.RenamePlaylistBottomSheet
import com.dn0ne.player.app.presentation.components.playlist.playlistCards
import com.dn0ne.player.app.presentation.components.trackList
import com.dn0ne.player.app.presentation.components.trackinfo.SearchField
import com.dn0ne.player.app.presentation.components.trackinfo.TrackInfoSheet
import com.kmpalette.rememberDominantColorState
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.ktx.toHct
import kotlinx.serialization.Serializable

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onCoverArtPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dominantColorState = rememberDominantColorState()
    var coverArtBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    DynamicMaterialTheme(
        seedColor = dominantColorState.color,
        primary = dominantColorState.color.takeIf { it.toHct().chroma <= 20 },
        animate = true
    ) {
        val rippleColor = MaterialTheme.colorScheme.primaryContainer
        val ripple = remember(rippleColor) {
            ripple(color = rippleColor)
        }
        val rippleConfiguration = remember(rippleColor) {
            RippleConfiguration(color = rippleColor)
        }
        CompositionLocalProvider(
            LocalIndication provides ripple,
            LocalRippleConfiguration provides rippleConfiguration
        ) {

            Box(
                modifier = modifier
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                val context = LocalContext.current
                val playbackState by viewModel.playbackState.collectAsState()
                val currentTrack by remember {
                    derivedStateOf {
                        playbackState.currentTrack
                    }
                }

                LaunchedEffect(currentTrack) {
                    if (currentTrack == null) {
                        dominantColorState.reset()
                    }
                }

                val trackSort by viewModel.trackSort.collectAsState()
                val trackSortOrder by viewModel.trackSortOrder.collectAsState()
                val playlistSort by viewModel.playlistSort.collectAsState()
                val playlistSortOrder by viewModel.playlistSortOrder.collectAsState()

                var showAddToOrCreatePlaylistSheet by rememberSaveable {
                    mutableStateOf(false)
                }
                var showCreatePlaylistOnly by rememberSaveable {
                    mutableStateOf(false)
                }
                var trackToAddToPlaylist by remember {
                    mutableStateOf<Track?>(null)
                }

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    enterTransition = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + slideInVertically(
                            initialOffsetY = { it / 4 })
                    },
                    exitTransition = {
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + slideOutVertically(
                            targetOffsetY = { it / 4 })
                    },
                    startDestination = PlayerRoutes.Main
                ) {
                    composable<PlayerRoutes.Main> {
                        val trackList by viewModel.trackList.collectAsState()
                        val playlists by viewModel.playlists.collectAsState()
                        val albumPlaylists by viewModel.albumPlaylists.collectAsState()
                        val artistPlaylists by viewModel.artistPlaylists.collectAsState()
                        val genrePlaylists by viewModel.genrePlaylists.collectAsState()

                        MainPlayerScreen(
                            trackList = trackList,
                            currentTrack = currentTrack,
                            onTrackClick = { track, playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnTrackClick(
                                        track = track,
                                        playlist = playlist
                                    )
                                )
                            },
                            onPlayNextClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(it))
                            },
                            onAddToQueueClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnAddToQueueClick(it))
                            },
                            onAddToPlaylistClick = {
                                showAddToOrCreatePlaylistSheet = true
                                showCreatePlaylistOnly = false
                                trackToAddToPlaylist = it
                            },
                            onViewTrackInfoClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnViewTrackInfoClick(it))
                            },
                            playlists = playlists,
                            albumPlaylists = albumPlaylists,
                            artistPlaylists = artistPlaylists,
                            genrePlaylists = genrePlaylists,
                            trackSort = trackSort,
                            trackSortOrder = trackSortOrder,
                            playlistSort = playlistSort,
                            playlistSortOrder = playlistSortOrder,
                            onTrackSortChange = { sort, order ->
                                viewModel.onEvent(PlayerScreenEvent.OnTrackSortChange(sort, order))
                            },
                            onPlaylistSortChange = { sort, order ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSortChange(
                                        sort,
                                        order
                                    )
                                )
                            },
                            onPlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist.copy(
                                            name = playlist.name
                                                ?: context.resources.getString(R.string.unknown)
                                        )
                                    )
                                )
                                navController.navigate(PlayerRoutes.MutablePlaylist)
                            },
                            onAlbumPlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist.copy(
                                            name = playlist.name
                                                ?: context.resources.getString(R.string.unknown_album)
                                        )
                                    )
                                )
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            onArtistPlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist.copy(
                                            name = playlist.name
                                                ?: context.resources.getString(R.string.unknown_artist)
                                        )
                                    )
                                )
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            onGenrePlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist.copy(
                                            name = playlist.name
                                                ?: context.resources.getString(R.string.unknown_genre)
                                        )
                                    )
                                )
                                navController.navigate(PlayerRoutes.Playlist)
                            }
                        )
                    }

                    composable<PlayerRoutes.Playlist> {
                        val playlist by viewModel.selectedPlaylist.collectAsState()
                        playlist?.let { playlist ->
                            Playlist(
                                playlist = playlist,
                                currentTrack = currentTrack,
                                onTrackClick = { track, playlist ->
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnTrackClick(
                                            track,
                                            playlist
                                        )
                                    )
                                },
                                onPlayNextClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(it))
                                },
                                onAddToQueueClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnAddToQueueClick(it))
                                },
                                onAddToPlaylistClick = {
                                    showAddToOrCreatePlaylistSheet = true
                                    showCreatePlaylistOnly = false
                                    trackToAddToPlaylist = it
                                },
                                onViewTrackInfoClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnViewTrackInfoClick(it))
                                },
                                trackSort = trackSort,
                                trackSortOrder = trackSortOrder,
                                onTrackSortChange = { sort, order ->
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnTrackSortChange(
                                            sort,
                                            order
                                        )
                                    )
                                },
                                onBackClick = {
                                    navController.navigateUp()
                                }
                            )
                        }
                    }

                    composable<PlayerRoutes.MutablePlaylist> {
                        var showRenameSheet by remember {
                            mutableStateOf(false)
                        }
                        var showDeleteDialog by remember {
                            mutableStateOf(false)
                        }
                        val playlists by viewModel.playlists.collectAsState()
                        val playlist by viewModel.selectedPlaylist.collectAsState()
                        playlist?.let { playlist ->
                            MutablePlaylist(
                                playlist = playlist,
                                currentTrack = currentTrack,
                                onRenamePlaylistClick = {
                                    showRenameSheet = true
                                },
                                onDeletePlaylistClick = {
                                    showDeleteDialog = true
                                },
                                onTrackClick = { track, playlist ->
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnTrackClick(
                                            track,
                                            playlist
                                        )
                                    )
                                },
                                onPlayNextClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(it))
                                },
                                onAddToQueueClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnAddToQueueClick(it))
                                },
                                onAddToPlaylistClick = {
                                    showAddToOrCreatePlaylistSheet = true
                                    showCreatePlaylistOnly = false
                                    trackToAddToPlaylist = it
                                },
                                onRemoveFromPlaylistClick = {
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnRemoveFromPlaylist(
                                            it,
                                            playlist
                                        )
                                    )
                                },
                                onViewTrackInfoClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnViewTrackInfoClick(it))
                                },
                                onTrackListReorder = {
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnPlaylistReorder(
                                            it,
                                            playlist
                                        )
                                    )
                                },
                                onBackClick = {
                                    navController.navigateUp()
                                }
                            )

                            if (showRenameSheet) {
                                RenamePlaylistBottomSheet(
                                    playlists = playlists,
                                    initialName = playlist.name ?: "",
                                    onRenameClick = {
                                        viewModel.onEvent(PlayerScreenEvent.OnRenamePlaylistClick(it, playlist))
                                    },
                                    onDismissRequest = {
                                        showRenameSheet = false
                                    }
                                )
                            }

                            if (showDeleteDialog) {
                                DeletePlaylistDialog(
                                    onConfirm = {
                                        showDeleteDialog = false
                                        navController.navigateUp()
                                        viewModel.onEvent(PlayerScreenEvent.OnDeletePlaylistClick(playlist))
                                    },
                                    onDismissRequest = {
                                        showDeleteDialog = false
                                    }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = currentTrack != null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                ) {
                    currentTrack?.let {

                        LaunchedEffect(coverArtBitmap) {
                            coverArtBitmap?.let {
                                dominantColorState.updateFrom(it)
                            }
                        }

                        PlayerSheet(
                            playbackStateFlow = viewModel.playbackState,
                            onPlayerExpandedChange = {
                                viewModel.onEvent(PlayerScreenEvent.OnPlayerExpandedChange(it))
                            },
                            onPlayClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnPlayClick)
                            },
                            onPauseClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnPauseClick)
                            },
                            onSeekToNextClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnSeekToNextClick)
                            },
                            onSeekToPreviousClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnSeekToPreviousClick)
                            },
                            onSeekTo = {
                                viewModel.onEvent(PlayerScreenEvent.OnSeekTo(it))
                            },
                            onPlaybackModeClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnPlaybackModeClick)
                            },
                            onCoverArtLoaded = {
                                coverArtBitmap = it
                            },
                            onPlayNextClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(currentTrack!!))
                            },
                            onAddToQueueClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnAddToQueueClick(currentTrack!!))
                            },
                            onAddToPlaylistClick = {
                                showAddToOrCreatePlaylistSheet = true
                                showCreatePlaylistOnly = false
                                trackToAddToPlaylist = it
                            },
                            onViewTrackInfoClick = {
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnViewTrackInfoClick(
                                        currentTrack!!
                                    )
                                )
                            },
                            onLyricsSheetExpandedChange = {
                                viewModel.onEvent(PlayerScreenEvent.OnLyricsSheetExpandedChange(it))
                            },
                            onLyricsClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnLyricsClick)
                            },
                            modifier = Modifier
                                .align(alignment = Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                }

                val trackInfoSheetState by viewModel.trackInfoSheetState.collectAsState()
                TrackInfoSheet(
                    state = trackInfoSheetState,
                    onCloseClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnCloseTrackInfoSheetClick)
                    },
                    onSearchInfo = { query ->
                        viewModel.onEvent(PlayerScreenEvent.OnSearchInfo(query))
                    },
                    onSearchResultClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnMetadataSearchResultPick(it))
                    },
                    onOverwriteMetadataClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnOverwriteMetadataClick(it))
                    },
                    onPickCoverArtClick = onCoverArtPick,
                    onRestoreCoverArtClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnRestoreCoverArtClick)
                    },
                    onConfirmMetadataEditClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnConfirmMetadataEditClick(it))
                    },
                    onRisksOfMetadataEditingAccept = {
                        viewModel.onEvent(PlayerScreenEvent.OnAcceptingRisksOfMetadataEditing)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                )

                if (showAddToOrCreatePlaylistSheet) {
                    val playlists by viewModel.playlists.collectAsState()
                    AddToOrCreatePlaylistBottomSheet(
                        playlists = playlists,
                        createOnly = showCreatePlaylistOnly,
                        onDismissRequest = {
                            showAddToOrCreatePlaylistSheet = false
                        },
                        onCreateClick = {
                            viewModel.onEvent(PlayerScreenEvent.OnCreatePlaylistClick(it))
                        },
                        onPlaylistSelection = { playlist ->
                            trackToAddToPlaylist?.let { track ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnAddToPlaylist(
                                        track = track,
                                        playlist = playlist
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainPlayerScreen(
    trackList: List<Track>,
    currentTrack: Track?,
    onTrackClick: (Track, Playlist) -> Unit,
    onPlayNextClick: (Track) -> Unit,
    onAddToQueueClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onViewTrackInfoClick: (Track) -> Unit,
    playlists: List<Playlist>,
    albumPlaylists: List<Playlist>,
    artistPlaylists: List<Playlist>,
    genrePlaylists: List<Playlist>,
    trackSort: TrackSort,
    trackSortOrder: SortOrder,
    playlistSort: PlaylistSort,
    playlistSortOrder: SortOrder,
    onTrackSortChange: (TrackSort?, SortOrder?) -> Unit,
    onPlaylistSortChange: (PlaylistSort?, SortOrder?) -> Unit,
    onPlaylistSelection: (Playlist) -> Unit,
    onAlbumPlaylistSelection: (Playlist) -> Unit,
    onArtistPlaylistSelection: (Playlist) -> Unit,
    onGenrePlaylistSelection: (Playlist) -> Unit,
) {
    val context = LocalContext.current

    val topBarTabs = remember {
        listOf(
            context.resources.getString(R.string.playlists),
            context.resources.getString(R.string.tracks),
            context.resources.getString(R.string.albums),
            context.resources.getString(R.string.artists),
            context.resources.getString(R.string.genres),
        )
    }

    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }

    var searchFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    var showSearchField by rememberSaveable {
        mutableStateOf(false)
    }

    LazyGridWithCollapsibleTabsTopBar(
        topBarTabTitles = topBarTabs,
        defaultSelectedTabIndex = 1,
        tabTitleTextStyle = MaterialTheme.typography.titleLarge.copy(
            fontSize = lerp(
                MaterialTheme.typography.titleLarge.fontSize,
                MaterialTheme.typography.displaySmall.fontSize,
                collapseFraction
            ),
            fontWeight = FontWeight.Bold
        ),
        topBarButtons = { tabIndex ->
            AnimatedContent(
                targetState = showSearchField,
                label = "top-bar-search-bar-animation",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .align(Alignment.BottomCenter)
            ) { state ->
                when (state) {
                    false -> {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                IconButton(
                                    onClick = {}
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = context.resources.getString(
                                            R.string.settings
                                        )
                                    )
                                }

                                if (tabIndex == 1) {
                                    TrackSortButton(
                                        sort = trackSort,
                                        order = trackSortOrder,
                                        onSortChange = {
                                            onTrackSortChange(it, null)
                                        },
                                        onSortOrderChange = {
                                            onTrackSortChange(null, it)
                                        }
                                    )
                                } else {
                                    PlaylistSortButton(
                                        sort = playlistSort,
                                        order = playlistSortOrder,
                                        onSortChange = {
                                            onPlaylistSortChange(it, null)
                                        },
                                        onSortOrderChange = {
                                            onPlaylistSortChange(null, it)
                                        }
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    showSearchField = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = context.resources.getString(
                                        R.string.track_search
                                    )
                                )
                            }
                        }
                    }

                    true -> {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SearchField(
                                value = searchFieldValue,
                                onValueChange = {
                                    searchFieldValue = it.trimStart()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                                    .align(Alignment.Center)
                            )
                            IconButton(
                                onClick = {
                                    showSearchField = false
                                    searchFieldValue = ""
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = context.resources.getString(
                                        R.string.close_track_search
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        collapseFraction = {
            collapseFraction = it
        },
        contentHorizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
        contentVerticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        gridCells = {
            if (it == 1) GridCells.Fixed(1) else {
                GridCells.Adaptive(150.dp)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) { tabIndex ->
        when (tabIndex) {
            0 -> {
                playlistCards(
                    playlists = playlists.filter {
                        if (searchFieldValue.isBlank()) return@filter true
                        it.name?.contains(
                            searchFieldValue,
                            ignoreCase = true
                        ) == true
                    },
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown),
                    showSinglePreview = false,
                    onCardClick = onPlaylistSelection,
                )
            }

            2 -> {
                playlistCards(
                    playlists = albumPlaylists.filter {
                        if (searchFieldValue.isBlank()) return@filter true
                        it.name?.contains(
                            searchFieldValue,
                            ignoreCase = true
                        ) == true
                    },
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_album),
                    showSinglePreview = true,
                    onCardClick = onAlbumPlaylistSelection,
                )
            }

            3 -> {
                playlistCards(
                    playlists = artistPlaylists.filter {
                        if (searchFieldValue.isBlank()) return@filter true
                        it.name?.contains(
                            searchFieldValue,
                            ignoreCase = true
                        ) == true
                    },
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_artist),
                    onCardClick = onArtistPlaylistSelection,
                )
            }

            4 -> {
                playlistCards(
                    playlists = genrePlaylists.filter {
                        if (searchFieldValue.isBlank()) return@filter true
                        it.name?.contains(
                            searchFieldValue,
                            ignoreCase = true
                        ) == true
                    },
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_genre),
                    onCardClick = onGenrePlaylistSelection,
                )
            }

            else -> {
                trackList(
                    trackList = trackList.filterTracks(searchFieldValue),
                    currentTrack = currentTrack,
                    onTrackClick = {
                        onTrackClick(
                            it,
                            Playlist(
                                name = null,
                                trackList = trackList
                            )
                        )
                    },
                    onPlayNextClick = onPlayNextClick,
                    onAddToQueueClick = onAddToQueueClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onViewTrackInfoClick = onViewTrackInfoClick
                )
            }
        }
    }
}

@Serializable
private sealed interface PlayerRoutes {
    @Serializable
    data object Main : PlayerRoutes

    @Serializable
    data object Playlist : PlayerRoutes

    @Serializable
    data object MutablePlaylist : PlayerRoutes
}
