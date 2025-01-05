package com.dn0ne.player.app.data.remote.lyrics

import android.content.Context
import android.util.Log
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.core.util.getAppVersionName
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.headers
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.Serializable
import java.net.SocketException
import java.nio.channels.UnresolvedAddressException

class LrclibLyricsProvider(
    context: Context,
    private val client: HttpClient
) : LyricsProvider {
    private val lrclibEndpoint = "https://lrclib.net/api"
    private val logTag = "LrclibLyricsProvider"
    private val userAgent =
        "${context.resources.getString(R.string.app_name)}/${context.getAppVersionName()} ( dev.dn0ne@gmail.com )"

    override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> {
        if (track.title == null || track.artist == null) {
            return Result.Error(DataError.Network.BadRequest)
        }

        val response = try {
            client.get(lrclibEndpoint) {
                url {
                    appendPathSegments("get")
                    parameters.append("track_name", track.title)
                    parameters.append("artist_name", track.artist)
                    track.album?.let {
                        parameters.append("album_name", it)
                    }
                    parameters.append("duration", (track.duration / 1000).toString())
                }
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    append(HttpHeaders.UserAgent, userAgent)
                }
            }
        } catch (e: UnresolvedAddressException) {
            Log.i(logTag, e.message.toString())
            return Result.Error(DataError.Network.NoInternet)
        } catch (_: HttpRequestTimeoutException) {
            return Result.Error(DataError.Network.RequestTimeout)
        } catch (_: SocketException) {
            return Result.Error(DataError.Network.Unknown)
        }

        when(response.status) {
            HttpStatusCode.OK -> {
                try {
                    val lyricsDto: LyricsDto = response.body()

                    val plainLyrics = lyricsDto.plainLyrics?.split('\n')
                    var syncedLyrics: List<Pair<Int, String>>? = null
                    try {
                        syncedLyrics = lyricsDto.syncedLyrics?.toSyncedLyrics()
                    } catch (e: IllegalArgumentException) {
                        Log.i(logTag, e.message, e)
                    }


                    return Result.Success(
                        data = Lyrics(
                            uri = track.uri.toString(),
                            plain = plainLyrics,
                            synced = syncedLyrics
                        )
                    )
                } catch (e: JsonConvertException) {
                    Log.d(logTag, e.message, e)
                    return Result.Error(DataError.Network.ParseError)
                }
            }

            HttpStatusCode.NotFound -> {
                return Result.Error(DataError.Network.NotFound)
            }

            else -> {
                return Result.Error(DataError.Network.Unknown)
            }
        }
    }
}

@Serializable
private data class LyricsDto(
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

private fun String.toSyncedLyrics(): List<Pair<Int, String>> {
    return split('\n').map {
        val timestampString = it.substring(1..8)
        val timestamp = timestampString.toLyricsTimestamp()

        timestamp to it.drop(11)
    }
}

private fun String.toLyricsTimestamp(): Int {
    val regex = Regex("""(\d+):(\d+)\.(\d+)""")
    regex.matchEntire(this)?.let { matchResult ->
        val minutes = matchResult.groupValues.getOrNull(1)?.toIntOrNull()
        val seconds = matchResult.groupValues.getOrNull(2)?.toIntOrNull()
        val centiseconds = matchResult.groupValues.getOrNull(3)?.toIntOrNull()

        if (minutes == null || seconds == null || centiseconds == null) {
            throw IllegalArgumentException("Failed to parse timestamp: $this")
        }

        return minutes * 60 * 1000 + seconds * 1000 + centiseconds * 10
    } ?: throw IllegalArgumentException("Failed to parse timestamp, does not match regex ${regex.pattern}")
}