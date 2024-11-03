package com.dn0ne.player.setup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dn0ne.player.R
import com.dn0ne.player.core.util.getAppVersionName

@Composable
fun WelcomePage(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        val context = LocalContext.current

        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(100.dp)
                )
            }

            Text(
                text = context.resources.getString(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = context.getAppVersionName()
            )
        }

        Button(
            onClick = onGetStartedClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Text(text = context.resources.getString(R.string.get_started))
        }
    }
}