package com.dn0ne.player.setup.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import com.dn0ne.player.setup.presentation.components.AudioPermissionPage
import com.dn0ne.player.setup.presentation.components.WelcomePage

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    modifier: Modifier = Modifier
) {
    val currentPage by viewModel.currentPage.collectAsState()

    AnimatedContent(
        targetState = currentPage,
        label = "setup screen page",
        modifier = modifier
    ) { page ->
        when(page) {
            SetupPage.Welcome -> {
                WelcomePage(
                    onGetStartedClick = {
                        viewModel.onEvent(SetupScreenEvent.OnNextClick)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            SetupPage.AudioPermission -> {
                AudioPermissionPage(
                    onGrantAudioPermissionClick = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
            SetupPage.LyricsProvider -> TODO()
            SetupPage.MetadataProvider -> TODO()
        }

    }
}