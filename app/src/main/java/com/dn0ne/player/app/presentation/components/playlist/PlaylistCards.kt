package com.dn0ne.player.app.presentation.components.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ShapeDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.sortedBy
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.presentation.components.PlaylistCard

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
            key = playlistsChunk.map { it.name }.joinToString()
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
                            title = playlist.name ?: fallbackPlaylistTitle,
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