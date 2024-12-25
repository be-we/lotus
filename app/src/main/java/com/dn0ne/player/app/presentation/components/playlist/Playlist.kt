package com.dn0ne.player.app.presentation.components.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.TrackSort
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.filterTracks
import com.dn0ne.player.app.presentation.components.topbar.LazyColumnWithCollapsibleTopBar
import com.dn0ne.player.app.presentation.components.TrackSortButton
import com.dn0ne.player.app.presentation.components.trackList
import com.dn0ne.player.app.presentation.components.trackinfo.SearchField

@Composable
fun Playlist(
    playlist: Playlist,
    currentTrack: Track?,
    onTrackClick: (Track, Playlist) -> Unit,
    onPlayNextClick: (Track) -> Unit,
    onAddToQueueClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onViewTrackInfoClick: (Track) -> Unit,
    trackSort: TrackSort,
    trackSortOrder: SortOrder,
    onTrackSortChange: (TrackSort?, SortOrder?) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current

    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }

    BackHandler {
        onBackClick()
    }

    var searchFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    var showSearchField by rememberSaveable {
        mutableStateOf(false)
    }

    LazyColumnWithCollapsibleTopBar(
        topBarContent = {
            Text(
                text = playlist.name
                    ?: context.resources.getString(R.string.unknown),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = lerp(
                        start = MaterialTheme.typography.titleLarge.fontSize,
                        stop = MaterialTheme.typography.headlineLarge.fontSize,
                        fraction = collapseFraction
                    ),
                ),
                softWrap = collapseFraction > .2f,
                overflow = if (collapseFraction > .2f) {
                    TextOverflow.Clip
                } else TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = if (collapseFraction > .2f) 28.dp else 108.dp)
            )

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
                                    onClick = onBackClick
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBackIosNew,
                                        contentDescription = context.resources.getString(
                                            R.string.back
                                        )
                                    )
                                }

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
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        trackList(
            trackList = playlist.trackList.filterTracks(searchFieldValue),
            currentTrack = currentTrack,
            onTrackClick = { track ->
                onTrackClick(track, playlist)
            },
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onViewTrackInfoClick = onViewTrackInfoClick
        )
    }
}