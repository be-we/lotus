package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.domain.lyrics.Lyrics
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RealmLyricsRepository(
    private val realm: Realm
): LyricsRepository {
    override fun getLyricsByUri(uri: String): Lyrics? {
        return realm.query<LyricsJson>("uri = $0", uri).find().firstOrNull()?.toLyrics()
    }

    override suspend fun insertLyrics(lyrics: Lyrics) {
        realm.write {
            copyToRealm(instance = lyrics.toLyricsJson(), updatePolicy = UpdatePolicy.ALL)
        }
    }
}

class LyricsJson(): RealmObject {
    var uri: String = ""
    var json: String = ""

    fun toLyrics(): Lyrics = Json.decodeFromString(json)
}

fun Lyrics.toLyricsJson(): LyricsJson = LyricsJson().apply {
    uri = this@toLyricsJson.uri
    json = Json.encodeToString(this@toLyricsJson)
}