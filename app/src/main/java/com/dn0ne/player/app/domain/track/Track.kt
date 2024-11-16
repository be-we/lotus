package com.dn0ne.player.app.domain.track

import android.net.Uri
import androidx.media3.common.MediaItem
import kotlinx.serialization.Serializable


@Serializable(with = TrackSerializer::class)
data class Track(
    val uri: Uri,
    val mediaItem: MediaItem,
    val title: String,
    val artist: String,
    val coverArtUri: Uri,
    val duration: Int,
)