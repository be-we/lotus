package com.dn0ne.player.app.presentation.components.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.filterTracks
import com.dn0ne.player.app.presentation.components.LazyColumnWithCollapsibleTopBar
import com.dn0ne.player.app.presentation.components.TrackListItem
import com.dn0ne.player.app.presentation.components.trackinfo.SearchField
import kotlinx.coroutines.FlowPreview
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(FlowPreview::class)
@Composable
fun MutablePlaylist(
    playlist: Playlist,
    currentTrack: Track?,
    onRenamePlaylistClick: () -> Unit,
    onDeletePlaylistClick: () -> Unit,
    onTrackClick: (Track, Playlist) -> Unit,
    onPlayNextClick: (Track) -> Unit,
    onAddToQueueClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onRemoveFromPlaylistClick: (Track) -> Unit,
    onViewTrackInfoClick: (Track) -> Unit,
    onTrackListReorder: (List<Track>) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

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

    var trackList by remember(playlist) {
        mutableStateOf(playlist.trackList)
    }
    val listState = rememberLazyListState()
    val reorderableListState = rememberReorderableLazyListState(listState) { from, to ->
        trackList = trackList.toMutableList().apply {
            add(to.index - 1, removeAt(from.index - 1))
        }
    }

    LaunchedEffect(reorderableListState.isAnyItemDragging) {
        if (!reorderableListState.isAnyItemDragging) {
            onTrackListReorder(trackList)
        }
    }

    LazyColumnWithCollapsibleTopBar(
        listState = listState,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                                IconButton(
                                    onClick = onDeletePlaylistClick
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = context.resources.getString(
                                            R.string.delete_playlist
                                        ) + " ${playlist.name}"
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onRenamePlaylistClick
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = context.resources.getString(
                                            R.string.rename_playlist
                                        ) + " ${playlist.name}"
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
        items(
            items = trackList.filterTracks(searchFieldValue),
            key = { "${it.uri}" }
        ) { track ->
            ReorderableItem(
                state = reorderableListState,
                key = "${track.uri}",
                modifier = Modifier.padding(horizontal = 16.dp)
            ) { isDragging ->
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.05f else 1f,
                    label = "dragged-track-scale-animation"
                )
                val backgroundColor by animateColorAsState(
                    targetValue = if (isDragging) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else Color.Transparent,
                    label = "dragged-track-back-animation"
                )
                TrackListItem(
                    track = track,
                    isCurrent = currentTrack == track,
                    onClick = {
                        onTrackClick(
                            track,
                            playlist
                        )
                    },
                    onPlayNextClick = { onPlayNextClick(track) },
                    onAddToQueueClick = { onAddToQueueClick(track) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                    onRemoveFromPlaylistClick = { onRemoveFromPlaylistClick(track) },
                    onViewTrackInfoClick = { onViewTrackInfoClick(track) },
                    dragHandle = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    ViewCompat.performHapticFeedback(
                                        view,
                                        HapticFeedbackConstantsCompat.GESTURE_START
                                    )
                                },
                                onDragStopped = {
                                    ViewCompat.performHapticFeedback(
                                        view,
                                        HapticFeedbackConstantsCompat.GESTURE_END
                                    )
                                }
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = context.resources.getString(R.string.reorder_track) + " ${track.title}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleY = scale
                            scaleX = scale
                            shadowElevation = scale
                        }
                        .clip(ShapeDefaults.Medium)
                        .background(
                            color = backgroundColor
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}