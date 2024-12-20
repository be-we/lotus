package com.dn0ne.player.app.presentation

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.sortedBy
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.LazyColumnWithCollapsibleTabsTopBar
import com.dn0ne.player.app.presentation.components.LazyColumnWithCollapsibleTopBar
import com.dn0ne.player.app.presentation.components.PlaylistCard
import com.dn0ne.player.app.presentation.components.PlaylistSortButton
import com.dn0ne.player.app.presentation.components.TrackListItem
import com.dn0ne.player.app.presentation.components.TrackSortButton
import com.dn0ne.player.app.presentation.components.playback.PlayerSheet
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
                var collapseFraction by remember {
                    mutableFloatStateOf(0f)
                }
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

                val topBarTabs = remember {
                    listOf(
                        context.resources.getString(R.string.tracks),
                        context.resources.getString(R.string.albums),
                        context.resources.getString(R.string.artists),
                        context.resources.getString(R.string.genres),
                    )
                }

                val trackSort by viewModel.trackSort.collectAsState()
                val trackSortOrder by viewModel.trackSortOrder.collectAsState()
                val playlistSort by viewModel.playlistSort.collectAsState()
                val playlistSortOrder by viewModel.playlistSortOrder.collectAsState()

                val trackList by viewModel.trackList.collectAsState()
                val albumPlaylists by viewModel.albumPlaylists.collectAsState()
                val artistPlaylists by viewModel.artistPlaylists.collectAsState()
                val genrePlaylists by viewModel.genrePlaylists.collectAsState()
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
                        LazyColumnWithCollapsibleTabsTopBar(
                            topBarTabTitles = topBarTabs,
                            tabTitleTextStyle = MaterialTheme.typography.titleLarge.copy(
                                fontSize = lerp(
                                    MaterialTheme.typography.titleLarge.fontSize,
                                    MaterialTheme.typography.displaySmall.fontSize,
                                    collapseFraction
                                ),
                                fontWeight = FontWeight.Bold
                            ),
                            topBarButtons = { tabIndex ->
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {}
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = context.resources.getString(R.string.settings)
                                        )
                                    }

                                    if (tabIndex == 0) {
                                        TrackSortButton(
                                            sort = trackSort,
                                            order = trackSortOrder,
                                            onSortChange = {
                                                viewModel.onEvent(
                                                    PlayerScreenEvent.OnTrackSortChange(
                                                        sort = it
                                                    )
                                                )
                                            },
                                            onSortOrderChange = {
                                                viewModel.onEvent(
                                                    PlayerScreenEvent.OnTrackSortChange(
                                                        order = it
                                                    )
                                                )
                                            }
                                        )
                                    } else {
                                        PlaylistSortButton(
                                            sort = playlistSort,
                                            order = playlistSortOrder,
                                            onSortChange = {
                                                viewModel.onEvent(
                                                    PlayerScreenEvent.OnPlaylistSortChange(
                                                        sort = it
                                                    )
                                                )
                                            },
                                            onSortOrderChange = {
                                                viewModel.onEvent(
                                                    PlayerScreenEvent.OnPlaylistSortChange(
                                                        order = it
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            },
                            collapseFraction = {
                                collapseFraction = it
                            },
                            contentHorizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .safeDrawingPadding()
                        ) { tabIndex ->
                            when (tabIndex) {
                                1 -> {
                                    playlistCards(
                                        playlists = albumPlaylists,
                                        sort = playlistSort,
                                        sortOrder = playlistSortOrder,
                                        fallbackPlaylistTitle = context.resources.getString(R.string.unknown_album),
                                        showSinglePreview = true,
                                        onCardClick = { playlist ->
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnPlaylistSelection(
                                                    playlist.copy(
                                                        title = playlist.title
                                                            ?: context.resources.getString(R.string.unknown_album)
                                                    )
                                                )
                                            )
                                            navController.navigate(PlayerRoutes.Playlist)
                                        },
                                        itemModifier = Modifier
                                            .padding(horizontal = 28.dp)
                                    )
                                }

                                2 -> {
                                    playlistCards(
                                        playlists = artistPlaylists,
                                        sort = playlistSort,
                                        sortOrder = playlistSortOrder,
                                        fallbackPlaylistTitle = context.resources.getString(R.string.unknown_artist),
                                        onCardClick = { playlist ->
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnPlaylistSelection(
                                                    playlist.copy(
                                                        title = playlist.title
                                                            ?: context.resources.getString(R.string.unknown_artist)
                                                    )
                                                )
                                            )
                                            navController.navigate(PlayerRoutes.Playlist)
                                        },
                                        itemModifier = Modifier
                                            .padding(horizontal = 28.dp)
                                    )
                                }

                                3 -> {
                                    playlistCards(
                                        playlists = genrePlaylists,
                                        sort = playlistSort,
                                        sortOrder = playlistSortOrder,
                                        fallbackPlaylistTitle = context.resources.getString(R.string.unknown_genre),
                                        onCardClick = { playlist ->
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnPlaylistSelection(
                                                    playlist.copy(
                                                        title = playlist.title
                                                            ?: context.resources.getString(R.string.unknown_genre)
                                                    )
                                                )
                                            )
                                            navController.navigate(PlayerRoutes.Playlist)
                                        },
                                        itemModifier = Modifier
                                            .padding(horizontal = 28.dp)
                                    )
                                }

                                else -> {
                                    trackList(
                                        trackList = trackList,
                                        currentTrack = currentTrack,
                                        onTrackClick = { track ->
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnTrackClick(
                                                    track,
                                                    trackList
                                                )
                                            )
                                        },
                                        onPlayNextClick = { track ->
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnPlayNextClick(
                                                    track
                                                )
                                            )
                                        },
                                        onAddToQueueClick = { track ->
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnAddToQueueClick(
                                                    track
                                                )
                                            )
                                        },
                                        onViewTrackInfoClick = { track ->
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnViewTrackInfoClick(
                                                    track
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    composable<PlayerRoutes.Playlist> {
                        var collapseFraction by remember {
                            mutableFloatStateOf(0f)
                        }

                        BackHandler {
                            navController.navigateUp()
                        }

                        val playlist by viewModel.selectedPlaylist.collectAsState()

                        LazyColumnWithCollapsibleTopBar(
                            topBarContent = {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            navController.navigateUp()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowBackIosNew,
                                            contentDescription = context.resources.getString(R.string.back)
                                        )
                                    }

                                    TrackSortButton(
                                        sort = trackSort,
                                        order = trackSortOrder,
                                        onSortChange = {
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnTrackSortChange(
                                                    sort = it
                                                )
                                            )
                                        },
                                        onSortOrderChange = {
                                            viewModel.onEvent(
                                                PlayerScreenEvent.OnTrackSortChange(
                                                    order = it
                                                )
                                            )
                                        }
                                    )
                                }

                                Text(
                                    text = playlist?.title
                                        ?: context.resources.getString(R.string.unknown),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = lerp(
                                            start = MaterialTheme.typography.titleLarge.fontSize,
                                            stop = MaterialTheme.typography.headlineLarge.fontSize,
                                            fraction = collapseFraction
                                        ),
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 28.dp)
                                )
                            },
                            collapseFraction = {
                                collapseFraction = it
                            },
                            contentHorizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .safeDrawingPadding()
                        ) {
                            playlist?.let { (_, trackList) ->
                                trackList(
                                    trackList = trackList,
                                    currentTrack = currentTrack,
                                    onTrackClick = { track ->
                                        viewModel.onEvent(
                                            PlayerScreenEvent.OnTrackClick(
                                                track,
                                                trackList
                                            )
                                        )
                                    },
                                    onPlayNextClick = { track ->
                                        viewModel.onEvent(
                                            PlayerScreenEvent.OnPlayNextClick(
                                                track
                                            )
                                        )
                                    },
                                    onAddToQueueClick = { track ->
                                        viewModel.onEvent(
                                            PlayerScreenEvent.OnAddToQueueClick(
                                                track
                                            )
                                        )
                                    },
                                    onViewTrackInfoClick = { track ->
                                        viewModel.onEvent(
                                            PlayerScreenEvent.OnViewTrackInfoClick(
                                                track
                                            )
                                        )
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
            }
        }
    }
}

fun LazyListScope.trackList(
    trackList: List<Track>,
    currentTrack: Track?,
    onTrackClick: (Track) -> Unit,
    onPlayNextClick: (Track) -> Unit,
    onAddToQueueClick: (Track) -> Unit,
    onViewTrackInfoClick: (Track) -> Unit,
) {
    items(
        items = trackList,
        key = { it.uri }
    ) { track ->
        TrackListItem(
            track = track,
            isCurrent = currentTrack == track,
            onClick = { onTrackClick(track) },
            onPlayNextClick = { onPlayNextClick(track) },
            onAddToQueueClick = { onAddToQueueClick(track) },
            onViewTrackInfoClick = { onViewTrackInfoClick(track) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .animateItem()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

fun LazyListScope.playlistCards(
    playlists: List<Playlist>,
    fallbackPlaylistTitle: String,
    sort: PlaylistSort,
    sortOrder: SortOrder,
    onCardClick: (Playlist) -> Unit,
    showSinglePreview: Boolean = false,
    itemModifier: Modifier = Modifier
) {
    playlists.sortedBy(sort, sortOrder).chunked(2).forEach { playlistsChunk ->
        item(
            key = playlistsChunk.map { it.title }.joinToString()
        ) {
            Column(
                modifier = Modifier.animateItem()
            ) {
                Row(
                    modifier = itemModifier,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    playlistsChunk.forEach { playlist ->
                        PlaylistCard(
                            title = playlist.title ?: fallbackPlaylistTitle,
                            trackCount = playlist.trackList.size,
                            coverArtPreviewUris = playlist.trackList
                                .take(
                                    if (showSinglePreview) 1 else 4
                                )
                                .map {
                                    it.coverArtUri
                                },
                            modifier = Modifier
                                .weight(1f)
                                .clip(ShapeDefaults.Large)
                                .clickable {
                                    onCardClick(playlist)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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
}
