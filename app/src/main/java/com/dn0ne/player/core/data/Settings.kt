package com.dn0ne.player.core.data

import android.content.Context

class Settings(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val themeKey = "theme"
    private val areRisksOfMetadataEditingAcceptedKey = "metadata-editing-dialog"
    private val matchDurationWhenSearchMetadataKey = "match-duration"

    var theme: Theme
        get() = Theme.entries[sharedPreferences.getInt(themeKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(themeKey, value.ordinal)
                apply()
            }
        }

    var areRisksOfMetadataEditingAccepted: Boolean
        get() = sharedPreferences.getBoolean(areRisksOfMetadataEditingAcceptedKey, false)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(areRisksOfMetadataEditingAcceptedKey, value)
                apply()
            }
        }

    var matchDurationWhenSearchMetadata: Boolean
        get() = sharedPreferences.getBoolean(matchDurationWhenSearchMetadataKey, true)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(matchDurationWhenSearchMetadataKey, value)
                apply()
            }
        }
}

enum class Theme {
    System,
    Light,
    Dark
}