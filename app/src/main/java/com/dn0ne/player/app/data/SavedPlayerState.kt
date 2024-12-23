package com.dn0ne.player.app.data

import android.content.Context
import com.dn0ne.player.app.domain.playback.PlaybackMode
import com.dn0ne.player.app.domain.track.Playlist
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SavedPlayerState(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("saved-player-state", Context.MODE_PRIVATE)

    private val playlistKey = "playlist"
    private val playbackModeKey = "playback-mode"

    var playlist: Playlist?
        get() {
            val playlistJson = sharedPreferences.getString(playlistKey, null)
            if (playlistJson == null) return null

            return Json.decodeFromString(playlistJson)
        }
        set(value) {
            val playlistJson = Json.encodeToString(value)
            with(sharedPreferences.edit()) {
                putString(playlistKey, playlistJson)
                apply()
            }
        }

    var playbackMode: PlaybackMode
        get() = PlaybackMode.entries[sharedPreferences.getInt(playbackModeKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(playbackModeKey, value.ordinal)
                apply()
            }
        }
}