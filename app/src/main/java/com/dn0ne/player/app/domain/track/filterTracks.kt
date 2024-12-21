package com.dn0ne.player.app.domain.track

fun List<Track>.filterTracks(query: String): List<Track> {
    return filter {
        if (query.isBlank()) return@filter true

        buildString {
            append(it.title + ' ')
            append(it.album + ' ')
            append(it.artist + ' ')
            append(it.albumArtist + ' ')
            append(it.genre + ' ')
            append(it.year + ' ')
        }.contains(query, ignoreCase = true)
    }
}