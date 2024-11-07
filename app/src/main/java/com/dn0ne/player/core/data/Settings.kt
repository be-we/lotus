package com.dn0ne.player.core.data

import android.content.Context

class Settings(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val themeKey = "theme"
    private val lyricsProviderKey = "lyrics-provider"
    private val metadataProviderKey = "metadata-provider"

    var theme: Theme
        get() = Theme.entries[sharedPreferences.getInt(themeKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(themeKey, value.ordinal)
                apply()
            }
        }

    var lyricsProvider: LyricsProvider
        get() = LyricsProvider.entries[sharedPreferences.getInt(lyricsProviderKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(lyricsProviderKey, value.ordinal)
                apply()
            }
        }

    var metadataProvider: MetadataProvider
        get() = MetadataProvider.entries[sharedPreferences.getInt(metadataProviderKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(metadataProviderKey, value.ordinal)
                apply()
            }
        }
}

enum class Theme {
    System,
    Light,
    Dark
}

enum class LyricsProvider {
    Musixmatch,
    LastFm
}

enum class MetadataProvider {
    LastFm,
    Deezer
}