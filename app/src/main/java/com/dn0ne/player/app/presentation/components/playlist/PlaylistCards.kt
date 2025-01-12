package com.dn0ne.player.app.presentation.components.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ShapeDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.sortedBy
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.presentation.components.NothingYet
import com.dn0ne.player.app.presentation.components.PlaylistCard

fun LazyGridScope.playlistCards(
    playlists: List<Playlist>,
    fallbackPlaylistTitle: String,
    sort: PlaylistSort,
    sortOrder: SortOrder,
    onCardClick: (Playlist) -> Unit,
    showSinglePreview: Boolean = false,
) {
    if (playlists.isEmpty()) {
        item(
            span = {
                GridItemSpan(maxLineSpan)
            }
        ) {
            NothingYet()
        }
    }

    items(
        items = playlists.sortedBy(sort, sortOrder),
        key = { "${it.name}-${it.trackList}" }
    ) { playlist ->
        PlaylistCard(
            title = playlist.name
                ?: fallbackPlaylistTitle,
            trackCount = playlist.trackList.size,
            coverArtPreviewUris = playlist.trackList
                .take(if (showSinglePreview) 1 else 4)
                .map { it.coverArtUri },
            modifier = Modifier
                .clip(ShapeDefaults.Large)
                .clickable {
                    onCardClick(playlist)
                }
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
        )
    }
}