package com.dn0ne.player.app.domain.sort

import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track

enum class SortOrder {
    Ascending, Descending
}

enum class TrackSort {
    Title, Album, Artist, Genre, Year, DateModified
}

fun List<Track>.sortedBy(sort: TrackSort, order: SortOrder): List<Track> {
    return when (order) {
        SortOrder.Ascending -> {
            when (sort) {
                TrackSort.Title -> sortedBy { it.title }
                TrackSort.Album -> sortedBy { it.album }
                TrackSort.Artist -> sortedBy { it.artist }
                TrackSort.Genre -> sortedBy { it.genre }
                TrackSort.Year -> sortedBy { it.year }
                TrackSort.DateModified -> sortedBy { it.dateModified }
            }
        }

        SortOrder.Descending -> {
            when (sort) {
                TrackSort.Title -> sortedByDescending { it.title }
                TrackSort.Album -> sortedByDescending { it.album }
                TrackSort.Artist -> sortedByDescending { it.artist }
                TrackSort.Genre -> sortedByDescending { it.genre }
                TrackSort.Year -> sortedByDescending { it.year }
                TrackSort.DateModified -> sortedByDescending { it.dateModified }
            }
        }
    }
}

enum class PlaylistSort {
    Title, TrackCount
}

fun List<Playlist>.sortedBy(
    sort: PlaylistSort,
    order: SortOrder
): List<Playlist> {
    return when(order) {
        SortOrder.Ascending -> {
            when(sort) {
                PlaylistSort.Title -> sortedBy { it.name }
                PlaylistSort.TrackCount -> sortedBy { it.trackList.size }
            }
        }
        SortOrder.Descending -> {
            when(sort) {
                PlaylistSort.Title -> sortedByDescending { it.name }
                PlaylistSort.TrackCount -> sortedByDescending { it.trackList.size }
            }
        }
    }
}