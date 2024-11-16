package com.dn0ne.player.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.PlayerSheet
import com.dn0ne.player.app.presentation.components.TrackListItem
import com.kmpalette.rememberDominantColorState
import com.materialkolor.DynamicMaterialTheme

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val dominantColorState = rememberDominantColorState()
    var coverArtBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    DynamicMaterialTheme(
        seedColor = dominantColorState.color,
        primary = dominantColorState.color,
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
                val playbackState by viewModel.playbackState.collectAsState()
                val currentTrack by remember {
                    derivedStateOf {
                        playbackState.currentTrack
                    }
                }

                val trackList by viewModel.trackList.collectAsState()
                TrackList(
                    currentTrack = currentTrack,
                    trackList = trackList,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .align(Alignment.TopCenter)
                )

                AnimatedVisibility(
                    visible = currentTrack != null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                ) {
                    LaunchedEffect(currentTrack) {
                        coverArtBitmap?.let {
                            dominantColorState.updateFrom(it)
                        }
                    }

                    PlayerSheet(
                        playbackStateFlow = viewModel.playbackState,
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
                        modifier = Modifier
                            .align(alignment = Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TrackList(
    currentTrack: Track?,
    trackList: List<Track>,
    onEvent: (PlayerScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tracks",
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = trackList,
                key = { it.uri }
            ) { track ->
                TrackListItem(
                    track = track,
                    isCurrent = currentTrack == track,
                    onClick = {
                        onEvent(PlayerScreenEvent.OnTrackClick(track, trackList))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}