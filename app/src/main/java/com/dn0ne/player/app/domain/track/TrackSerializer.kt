package com.dn0ne.player.app.domain.track

import android.net.Uri
import androidx.media3.common.MediaItem
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object TrackSerializer : KSerializer<Track> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("Track") {
            element<String>("uri")
            element<String>("title")
            element<String>("artist")
            element<String>("coverArtUri")
            element<Int>("duration")
        }

    override fun serialize(
        encoder: Encoder,
        value: Track
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.uri.toString())
            encodeStringElement(descriptor, 1, value.title)
            encodeStringElement(descriptor, 2, value.artist)
            encodeStringElement(descriptor, 3, value.coverArtUri.toString())
            encodeIntElement(descriptor, 4, value.duration)
        }
    }

    override fun deserialize(decoder: Decoder): Track =
        decoder.decodeStructure(descriptor) {
            var uriString = ""
            var title = ""
            var artist = ""
            var coverArtUriString = ""
            var duration = -1

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> uriString = decodeStringElement(descriptor, 0)
                    1 -> title = decodeStringElement(descriptor, 1)
                    2 -> artist = decodeStringElement(descriptor, 2)
                    3 -> coverArtUriString = decodeStringElement(descriptor, 3)
                    4 -> duration = decodeIntElement(descriptor, 4)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            require(uriString.isNotBlank() && coverArtUriString.isNotBlank() && duration >= 0)

            val uri = Uri.parse(uriString)
            val mediaItem = MediaItem.fromUri(uri)
            val coverArtUri = Uri.parse(coverArtUriString)
            Track(
                uri = Uri.parse(uriString),
                mediaItem = mediaItem,
                title = title,
                artist = artist,
                coverArtUri = coverArtUri,
                duration = duration
            )
        }
}