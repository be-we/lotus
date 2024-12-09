package com.dn0ne.player.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.LazyColumnWithCollapsibleTopBar
import com.dn0ne.player.app.presentation.components.TrackListItem
import com.dn0ne.player.app.presentation.components.playback.PlayerSheet
import com.dn0ne.player.app.presentation.components.trackinfo.TrackInfoSheet
import com.kmpalette.rememberDominantColorState
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.ktx.toHct

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

                val trackList by viewModel.trackList.collectAsState()
                LazyColumnWithCollapsibleTopBar(
                    topBarContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .safeDrawingPadding()
                        ) {
                            Text(
                                text = context.resources.getString(R.string.tracks),
                                fontSize = lerp(
                                    MaterialTheme.typography.titleLarge.fontSize,
                                    MaterialTheme.typography.displaySmall.fontSize,
                                    collapseFraction
                                ),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    },
                    collapseFraction = {
                        collapseFraction = it
                    },
                    contentHorizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.safeDrawingPadding()
                ) {
                    items(
                        items = trackList,
                        key = { it.uri }
                    ) { track ->
                        TrackListItem(
                            track = track,
                            isCurrent = currentTrack == track,
                            onClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnTrackClick(track, trackList))
                            },
                            onPlayNextClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(track))
                            },
                            onAddToQueueClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnAddToQueueClick(track))
                            },
                            onViewTrackInfoClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnViewTrackInfoClick(track))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
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
