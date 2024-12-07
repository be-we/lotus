package com.dn0ne.player.app.domain.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val uri: String,
    val plain: List<String>? = null,
    val synced: List<Pair<Int, String>>? = null
)
