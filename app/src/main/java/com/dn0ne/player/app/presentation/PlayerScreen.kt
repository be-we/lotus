package com.dn0ne.player.app.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.TrackSort
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.filterPlaylists
import com.dn0ne.player.app.domain.track.filterTracks
import com.dn0ne.player.app.presentation.components.PlaylistSortButton
import com.dn0ne.player.app.presentation.components.TrackSortButton
import com.dn0ne.player.app.presentation.components.playback.PlayerSheet
import com.dn0ne.player.app.presentation.components.playlist.AddToOrCreatePlaylistBottomSheet
import com.dn0ne.player.app.presentation.components.playlist.DeletePlaylistDialog
import com.dn0ne.player.app.presentation.components.playlist.MutablePlaylist
import com.dn0ne.player.app.presentation.components.playlist.Playlist
import com.dn0ne.player.app.presentation.components.playlist.RenamePlaylistBottomSheet
import com.dn0ne.player.app.presentation.components.playlist.playlistCards
import com.dn0ne.player.app.presentation.components.settings.SettingsSheet
import com.dn0ne.player.app.presentation.components.settings.Theme
import com.dn0ne.player.app.presentation.components.topbar.LazyGridWithCollapsibleTabsTopBar
import com.dn0ne.player.app.presentation.components.trackList
import com.dn0ne.player.app.presentation.components.trackinfo.SearchField
import com.dn0ne.player.app.presentation.components.trackinfo.TrackInfoSheet
import com.kmpalette.color
import com.kmpalette.rememberDominantColorState
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.toHct
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onCoverArtPick: () -> Unit,
    onFolderPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val useDynamicColor by viewModel.settings.useDynamicColor.collectAsState()
    val useAlbumArtColor by viewModel.settings.useAlbumArtColor.collectAsState()
    val dominantColorState = rememberDominantColorState()
    var coverArtBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    val colorToApply by remember(useAlbumArtColor, useDynamicColor) {
        derivedStateOf {
            if (useAlbumArtColor) {
                dominantColorState.result
                    ?.paletteOrNull
                    ?.swatches
                    ?.sortedByDescending { it.population }
                    ?.let { swatches ->
                        val firstSwatch = swatches.first()
                        val firstSwatchColorHct = firstSwatch.color.toHct()
                        val firstSwatchPopulation = firstSwatch.population
                        val moreChromatic = swatches.fastFirstOrNull {
                            it.color.toHct().chroma - firstSwatchColorHct.chroma >= 30 &&
                                    it.population.toFloat() / firstSwatchPopulation >= .1f
                        }
                        moreChromatic?.color ?: firstSwatch.color
                    } ?: dominantColorState.color
            } else dominantColorState.color
        }
    }

    LaunchedEffect(useAlbumArtColor, useDynamicColor) {
        if (useAlbumArtColor) {
            coverArtBitmap?.let {
                dominantColorState.updateFrom(it)
            }
        } else dominantColorState.reset()
    }

    val appearance by viewModel.settings.appearance.collectAsState()
    val amoledDarkTheme by viewModel.settings.amoledDarkTheme.collectAsState()
    val paletteStyle by viewModel.settings.paletteStyle.collectAsState()
    DynamicMaterialTheme(
        seedColor = colorToApply,
        primary = colorToApply.takeIf { it.toHct().chroma <= 20 },
        useDarkTheme = when (appearance) {
            Theme.Appearance.System -> isSystemInDarkTheme()
            Theme.Appearance.Light -> false
            Theme.Appearance.Dark -> true
        },
        withAmoled = amoledDarkTheme,
        style = when (paletteStyle) {
            Theme.PaletteStyle.TonalSpot -> PaletteStyle.TonalSpot
            Theme.PaletteStyle.Neutral -> PaletteStyle.Neutral
            Theme.PaletteStyle.Vibrant -> PaletteStyle.Vibrant
            Theme.PaletteStyle.Expressive -> PaletteStyle.Expressive
            Theme.PaletteStyle.Rainbow -> PaletteStyle.Rainbow
            Theme.PaletteStyle.FruitSalad -> PaletteStyle.FruitSalad
            Theme.PaletteStyle.Monochrome -> PaletteStyle.Monochrome
            Theme.PaletteStyle.Fidelity -> PaletteStyle.Fidelity
            Theme.PaletteStyle.Content -> PaletteStyle.Content
        },
        animationSpec = tween(300, 200),
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
            LocalRippleConfiguration provides rippleConfiguration,
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
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

                var showScrollToTopButton by remember {
                    mutableStateOf(false)
                }
                var onScrollToTopClick by remember {
                    mutableStateOf(suspend {})
                }
                var showLocateButton by remember {
                    mutableStateOf(false)
                }
                var onLocateClick by remember {
                    mutableStateOf(suspend {})
                }

                NavHost(
                    navController = navController,
                    enterTransition = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideInVertically(initialOffsetY = { it / 10 })
                    },
                    exitTransition = {
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideOutVertically(targetOffsetY = { -it / 10 })
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideInVertically(initialOffsetY = { -it / 10 })
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideOutVertically(targetOffsetY = { it / 10 })
                    },
                    startDestination = PlayerRoutes.Main
                ) {
                    composable<PlayerRoutes.Main> {
                        val trackList by viewModel.trackList.collectAsState()
                        val playlists by viewModel.playlists.collectAsState()
                        val albumPlaylists by viewModel.albumPlaylists.collectAsState()
                        val artistPlaylists by viewModel.artistPlaylists.collectAsState()
                        val genrePlaylists by viewModel.genrePlaylists.collectAsState()
                        val folderPlaylists by viewModel.folderPlaylists.collectAsState()

                        val gridState = rememberLazyGridState()
                        var selectedTabIndex by rememberSaveable {
                            mutableIntStateOf(1)
                        }
                        val shouldShowLocateButton by remember(currentTrack, trackList) {
                            derivedStateOf {
                                selectedTabIndex == 1 &&
                                        currentTrack != null &&
                                        gridState.layoutInfo.visibleItemsInfo.fastFirstOrNull {
                                            it.index == trackList.indexOf(currentTrack)
                                        } == null
                            }
                        }
                        onLocateClick = remember(currentTrack, trackList) {
                            {
                                val currentTrackIndex = trackList.indexOf(currentTrack)
                                val preAnimateItemIndex = if (
                                    gridState.firstVisibleItemIndex < currentTrackIndex
                                ) {
                                    (currentTrackIndex - 5).coerceAtLeast(0)
                                } else currentTrackIndex + 5
                                gridState.scrollToItem(preAnimateItemIndex)
                                gridState.animateScrollToItem(currentTrackIndex)
                            }
                        }
                        LaunchedEffect(shouldShowLocateButton) {
                            showLocateButton = shouldShowLocateButton
                        }

                        val isScrolledEnough by remember {
                            derivedStateOf {
                                gridState.firstVisibleItemIndex >= 5
                            }
                        }
                        onScrollToTopClick = {
                            gridState.scrollToItem(5)
                            gridState.animateScrollToItem(0)
                        }

                        LaunchedEffect(isScrolledEnough) {
                            showScrollToTopButton = isScrolledEnough
                        }

                        MainPlayerScreen(
                            gridState = gridState,
                            onTabChange = {
                                selectedTabIndex = it
                            },
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
                            folderPlaylists = folderPlaylists,
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
                            },
                            onFolderPlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist
                                    )
                                )
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            onSettingsClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnSettingsClick)
                            }
                        )
                    }

                    composable<PlayerRoutes.Playlist> {
                        val listState = rememberLazyListState()
                        val isScrolledEnough by remember {
                            derivedStateOf {
                                listState.firstVisibleItemIndex >= 5
                            }
                        }
                        onScrollToTopClick = {
                            listState.scrollToItem(5)
                            listState.animateScrollToItem(0)
                        }

                        LaunchedEffect(isScrolledEnough) {
                            showScrollToTopButton = isScrolledEnough
                        }

                        val playlist by viewModel.selectedPlaylist.collectAsState()
                        playlist?.let { playlist ->
                            val shouldShowLocateButton by remember(currentTrack, playlist) {
                                derivedStateOf {
                                    val index = playlist.trackList.indexOf(currentTrack)
                                    currentTrack != null &&
                                            index >= 0 &&
                                            listState.layoutInfo.visibleItemsInfo.fastFirstOrNull {
                                                it.index == index
                                            } == null
                                }
                            }
                            onLocateClick = remember(currentTrack, playlist) {
                                {
                                    val currentTrackIndex = playlist.trackList.indexOf(currentTrack)
                                    val preAnimateItemIndex = if (
                                        listState.firstVisibleItemIndex < currentTrackIndex
                                    ) {
                                        (currentTrackIndex - 5).coerceAtLeast(0)
                                    } else currentTrackIndex + 5
                                    listState.scrollToItem(preAnimateItemIndex)
                                    listState.animateScrollToItem(currentTrackIndex)
                                }
                            }
                            LaunchedEffect(shouldShowLocateButton) {
                                showLocateButton = shouldShowLocateButton
                            }

                            Playlist(
                                listState = listState,
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

                        val listState = rememberLazyListState()
                        val isScrolledEnough by remember {
                            derivedStateOf {
                                listState.firstVisibleItemIndex >= 5
                            }
                        }
                        onScrollToTopClick = {
                            listState.scrollToItem(5)
                            listState.animateScrollToItem(0)
                        }

                        LaunchedEffect(isScrolledEnough) {
                            showScrollToTopButton = isScrolledEnough
                        }

                        val playlists by viewModel.playlists.collectAsState()
                        val playlist by viewModel.selectedPlaylist.collectAsState()
                        playlist?.let { playlist ->
                            var changedTrackList by remember {
                                mutableStateOf(playlist.trackList)
                            }
                            val shouldShowLocateButton by remember(currentTrack, changedTrackList) {
                                derivedStateOf {
                                    val index = changedTrackList.indexOf(currentTrack)
                                    currentTrack != null &&
                                            index >= 0 &&
                                            listState.layoutInfo.visibleItemsInfo.fastFirstOrNull {
                                                it.index == index
                                            } == null
                                }
                            }
                            onLocateClick = remember(currentTrack, changedTrackList) {
                                {
                                    val currentTrackIndex = changedTrackList.indexOf(currentTrack)
                                    val preAnimateItemIndex = if (
                                        listState.firstVisibleItemIndex < currentTrackIndex
                                    ) {
                                        (currentTrackIndex - 5).coerceAtLeast(0)
                                    } else currentTrackIndex + 5
                                    listState.scrollToItem(preAnimateItemIndex)
                                    listState.animateScrollToItem(currentTrackIndex)
                                }
                            }
                            LaunchedEffect(shouldShowLocateButton) {
                                showLocateButton = shouldShowLocateButton
                            }

                            MutablePlaylist(
                                listState = listState,
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
                                    changedTrackList = it
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
                                        viewModel.onEvent(
                                            PlayerScreenEvent.OnRenamePlaylistClick(
                                                it,
                                                playlist
                                            )
                                        )
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
                                        viewModel.onEvent(
                                            PlayerScreenEvent.OnDeletePlaylistClick(
                                                playlist
                                            )
                                        )
                                    },
                                    onDismissRequest = {
                                        showDeleteDialog = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    val isPlayerExpanded by remember {
                        derivedStateOf { playbackState.isPlayerExpanded }
                    }
                    if (!isPlayerExpanded) {
                        ScrollToTopAndLocateButtons(
                            showScrollToTopButton = showScrollToTopButton,
                            onScrollToTopClick = onScrollToTopClick,
                            showLocateButton = showLocateButton,
                            onLocateClick = onLocateClick,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    AnimatedVisibility(
                        visible = currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier
                            .align(alignment = Alignment.CenterHorizontally)
                    ) {
                        currentTrack?.let {

                            if (useAlbumArtColor) {
                                LaunchedEffect(coverArtBitmap) {
                                    coverArtBitmap?.let {
                                        dominantColorState.updateFrom(it)
                                    }
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
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnAddToQueueClick(
                                            currentTrack!!
                                        )
                                    )
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
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnLyricsSheetExpandedChange(
                                            it
                                        )
                                    )
                                },
                                onLyricsClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnLyricsClick)
                                },
                                settings = viewModel.settings,
                                modifier = Modifier
                                    .align(alignment = Alignment.CenterHorizontally)
                                    .fillMaxWidth()
                            )
                        }
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

                val settingsSheetState by viewModel.settingsSheetState.collectAsState()
                SettingsSheet(
                    state = settingsSheetState,
                    onFolderPick = onFolderPick,
                    onScanFoldersClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnScanFoldersClick)
                    },
                    onCloseClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnCloseSettingsClick)
                    },
                    dominantColorState = dominantColorState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MainPlayerScreen(
    gridState: LazyGridState = rememberLazyGridState(),
    onTabChange: (Int) -> Unit = {},
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
    folderPlaylists: List<Playlist>,
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
    onFolderPlaylistSelection: (Playlist) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current

    val topBarTabs = remember {
        listOf(
            context.resources.getString(R.string.playlists),
            context.resources.getString(R.string.tracks),
            context.resources.getString(R.string.albums),
            context.resources.getString(R.string.artists),
            context.resources.getString(R.string.genres),
            context.resources.getString(R.string.folders)
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
        gridState = gridState,
        topBarTabTitles = topBarTabs,
        defaultSelectedTabIndex = 1,
        onTabChange = {
            showSearchField = false
            searchFieldValue = ""
            onTabChange(it)
        },
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
                                    onClick = onSettingsClick
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
                        BackHandler {
                            showSearchField = false
                            searchFieldValue = ""
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val focusRequester = remember {
                                FocusRequester()
                            }
                            SearchField(
                                value = searchFieldValue,
                                onValueChange = {
                                    searchFieldValue = it.trimStart()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                                    .align(Alignment.Center)
                                    .focusRequester(focusRequester)
                            )

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }

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
        contentHorizontalArrangement = Arrangement.spacedBy(
            16.dp,
            alignment = Alignment.CenterHorizontally
        ),
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
                    playlists = playlists.filterPlaylists(searchFieldValue),
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown),
                    showSinglePreview = false,
                    onCardClick = onPlaylistSelection,
                )
            }

            2 -> {
                playlistCards(
                    playlists = albumPlaylists.filterPlaylists(searchFieldValue),
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_album),
                    showSinglePreview = true,
                    onCardClick = onAlbumPlaylistSelection,
                )
            }

            3 -> {
                playlistCards(
                    playlists = artistPlaylists.filterPlaylists(searchFieldValue),
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_artist),
                    onCardClick = onArtistPlaylistSelection,
                )
            }

            4 -> {
                playlistCards(
                    playlists = genrePlaylists.filterPlaylists(searchFieldValue),
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_genre),
                    onCardClick = onGenrePlaylistSelection,
                )
            }

            5 -> {
                playlistCards(
                    playlists = folderPlaylists.filterPlaylists(searchFieldValue),
                    sort = playlistSort,
                    sortOrder = playlistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_folder),
                    onCardClick = onFolderPlaylistSelection
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

@Composable
fun ScrollToTopAndLocateButtons(
    showScrollToTopButton: Boolean,
    onScrollToTopClick: suspend () -> Unit,
    showLocateButton: Boolean,
    onLocateClick: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.End
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        AnimatedVisibility(
            visible = showLocateButton,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            FilledTonalIconButton(
                onClick = {
                    coroutineScope.launch {
                        onLocateClick()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = context.resources.getString(R.string.scroll_to_current_track)
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTopButton,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            FilledTonalIconButton(
                onClick = {
                    coroutineScope.launch {
                        onScrollToTopClick()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = context.resources.getString(R.string.scroll_to_top)
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
