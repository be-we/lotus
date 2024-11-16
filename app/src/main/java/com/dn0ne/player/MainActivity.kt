package com.dn0ne.player

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.transitionFactory
import coil3.transition.CrossfadeTransition
import com.dn0ne.player.app.presentation.PlayerScreen
import com.dn0ne.player.app.presentation.PlayerViewModel
import com.dn0ne.player.core.presentation.Routes
import com.dn0ne.player.setup.data.SetupState
import com.dn0ne.player.setup.presentation.SetupScreen
import com.dn0ne.player.setup.presentation.SetupViewModel
import com.dn0ne.player.ui.theme.MusicPlayerTheme
import com.google.common.util.concurrent.MoreExecutors
import okio.Path.Companion.toPath
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.getViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(applicationContext)
                .transitionFactory(CrossfadeTransition.Factory())
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(applicationContext, 0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(applicationContext.cacheDir.resolve("image_cache").path.toPath())
                        .maxSizePercent(0.02)
                        .build()
                }
                .build()
        }

        val setupViewModel = getViewModel<SetupViewModel>()
        setupViewModel.onAudioPermissionRequest(checkAudioPermission())

        val setupState = get<SetupState>()
        val settingsToast = Toast.makeText(
            this,
            resources.getString(R.string.grant_permission_in_settings),
            Toast.LENGTH_SHORT
        )

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    setupViewModel.onAudioPermissionRequest(true)
                } else {
                    settingsToast.show()
                    goToAppSettings()
                }
            }

        val startDestination = if (checkAudioPermission() && setupState.isComplete) {
            Routes.Player
        } else Routes.Setup

        setContent {
            MusicPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable<Routes.Setup> {
                            SetupScreen(
                                viewModel = setupViewModel,
                                requestAudioPermission = {
                                    when {
                                        checkAudioPermission() -> {
                                            setupViewModel.onAudioPermissionRequest(true)
                                        }

                                        else -> {
                                            requestPermissionLauncher.launch(
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    Manifest.permission.READ_MEDIA_AUDIO
                                                } else Manifest.permission.READ_EXTERNAL_STORAGE,
                                            )
                                        }
                                    }
                                },
                                onFinishSetupClick = {
                                    navController.navigate(Routes.Player) {
                                        popUpTo(Routes.Setup) {
                                            inclusive = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }

                        composable<Routes.Player> {
                            val viewModel = getViewModel<PlayerViewModel>()
                            PlayerScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val viewModel = getViewModel<PlayerViewModel>()
        val mediaSessionToken =
            SessionToken(application, ComponentName(application, PlaybackService::class.java))

        val controllerFuture = MediaController.Builder(application, mediaSessionToken).buildAsync()
        controllerFuture.addListener(
            {
                viewModel.player = controllerFuture.get()
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun checkAudioPermission(): Boolean =
        checkSelfPermission(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun goToAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}