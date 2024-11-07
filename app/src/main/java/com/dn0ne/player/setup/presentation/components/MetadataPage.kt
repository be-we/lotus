package com.dn0ne.player.setup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArtTrack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dn0ne.player.R
import com.dn0ne.player.core.data.MetadataProvider
import com.dn0ne.player.setup.presentation.SetupScreenEvent

@Composable
fun MetadataPage(
    selectedMetadataProviderState: State<MetadataProvider>,
    onEvent: (SetupScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(28.dp)
    ) {
        val context = LocalContext.current

        Column(
            modifier = Modifier.align(alignment = Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(175.dp))

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArtTrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(75.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = context.resources.getString(R.string.metadata_source),
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = context.resources.getString(R.string.explain_metadata),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify
            )

            Spacer(modifier = Modifier.height(16.dp))

            val selectedMetadataProvider by selectedMetadataProviderState
            MetadataProvider.entries.forEach { provider ->
                SettingsItem(
                    isSelected = selectedMetadataProvider == provider,
                    title = provider.name,
                    description = "",
                    icon = Icons.Rounded.ArtTrack,
                    onClick = {
                        onEvent(SetupScreenEvent.OnMetadataProviderClick(provider))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Button(
            onClick = {},
            modifier = Modifier.align(alignment = Alignment.BottomEnd)
        ) {
            Text(
                text = context.resources.getString(R.string.next)
            )
        }
    }
}