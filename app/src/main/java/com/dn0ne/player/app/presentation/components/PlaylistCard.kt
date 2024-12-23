package com.dn0ne.player.app.presentation.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmpalette.rememberDominantColorState
import kotlinx.coroutines.launch

@Composable
fun PlaylistCard(
    title: String,
    trackCount: Int,
    coverArtPreviewUris: List<Uri>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            if (coverArtPreviewUris.size <= 1) {
                val dominantColorState = rememberDominantColorState()
                val coroutineScope = rememberCoroutineScope()
                CoverArt(
                    uri = coverArtPreviewUris.firstOrNull() ?: Uri.EMPTY,
                    onCoverArtLoaded = { bitmap ->
                        bitmap?.let {
                            coroutineScope.launch {
                                dominantColorState.updateFrom(it)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeDefaults.Large)
                )

                TrackCountBubble(
                    trackCount = trackCount,
                    contentColor = dominantColorState.onColor,
                    containerColor = dominantColorState.color,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                )
            } else {
                FourArtsPreview(
                    coverArtPreviewUris
                )

                TrackCountBubble(
                    trackCount = trackCount,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FourArtsPreview(
    coverArtPreviewUris: List<Uri>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(ShapeDefaults.Large)
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {
        Row {
            coverArtPreviewUris.getOrNull(0)?.let {
                CoverArt(
                    uri = it,
                    modifier = Modifier
                        .weight(1f)
                        .clip(ShapeDefaults.Small)
                )
            } ?: Box(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.width(8.dp))

            coverArtPreviewUris.getOrNull(1)?.let {
                CoverArt(
                    uri = it,
                    modifier = Modifier
                        .weight(1f)
                        .clip(ShapeDefaults.Small)
                )
            } ?: Box(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            coverArtPreviewUris.getOrNull(2)?.let {
                CoverArt(
                    uri = it,
                    modifier = Modifier
                        .weight(1f)
                        .clip(ShapeDefaults.Small)
                )
            } ?: Box(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.width(8.dp))

            coverArtPreviewUris.getOrNull(3)?.let {
                CoverArt(
                    uri = it,
                    modifier = Modifier
                        .weight(1f)
                        .clip(ShapeDefaults.Small)
                )
            } ?: Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun TrackCountBubble(
    trackCount: Int,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.ExtraLarge)
            .background(color = containerColor)
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = .1f),
                shape = ShapeDefaults.ExtraLarge
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = "$trackCount",
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}