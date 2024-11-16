package com.dn0ne.player.setup.presentation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dn0ne.player.setup.presentation.components.AudioPermissionPage
import com.dn0ne.player.setup.presentation.components.MetadataPage
import com.dn0ne.player.setup.presentation.components.WelcomePage

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    requestAudioPermission: () -> Unit,
    onFinishSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val startDestination = viewModel.startDestination

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it })

        }
    ) {
        composable<SetupPage.Welcome> {
            WelcomePage(
                onGetStartedClick = {
                    navController.navigate(SetupPage.AudioPermission)
                },
                modifier = modifier.fillMaxSize()
            )
        }

        composable<SetupPage.AudioPermission> {
            val isAudioPermissionGranted = viewModel.isAudioPermissionGranted.collectAsState()
            AudioPermissionPage(
                onGrantAudioPermissionClick = requestAudioPermission,
                onNextClick = {
                    viewModel.onEvent(SetupScreenEvent.OnFinishSetupClick)
                    onFinishSetupClick()
                },
                isAudioPermissionGrantedState = isAudioPermissionGranted,
                modifier = modifier.fillMaxSize()
            )
        }

        composable<SetupPage.MetadataProvider> {
            val selectedMetadataProviderState = viewModel.selectedMetadataProvider.collectAsState()
            MetadataPage(
                selectedMetadataProviderState = selectedMetadataProviderState,
                onEvent = viewModel::onEvent,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}