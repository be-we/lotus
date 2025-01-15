package com.dn0ne.player.app.presentation.components.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.core.data.Settings

@Composable
fun MusicScanSettings(
    settings: Settings,
    foldersWithAudio: Set<String>,
    onFolderPick: () -> Unit,
    onScanFoldersClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }

    ColumnWithCollapsibleTopBar(
        topBarContent = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = context.resources.getString(R.string.back)
                )
            }

            Text(
                text = context.resources.getString(R.string.music_scan),
                fontSize = lerp(
                    MaterialTheme.typography.titleLarge.fontSize,
                    MaterialTheme.typography.displaySmall.fontSize,
                    collapseFraction
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            )
        },
        collapseFraction = {
            collapseFraction = it
        },
        contentPadding = PaddingValues(horizontal = 28.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        MusicScanSettingsContent(
            settings = settings,
            onFolderPick = onFolderPick,
            foldersWithAudio = foldersWithAudio,
            onScanFoldersClick = onScanFoldersClick
        )
    }
}

@Composable
fun MusicScanSettingsContent(
    settings: Settings,
    foldersWithAudio: Set<String>,
    onFolderPick: () -> Unit,
    onScanFoldersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
    ) {
        val isScanModeInclusive by settings.isScanModeInclusive.collectAsState()
        val context = LocalContext.current
        val scanModeOptions = remember {
            listOf(
                SettingSegmentOption(
                    icon = Icons.Rounded.Add,
                    contentDescription = context.resources.getString(R.string.inclusive),
                    onClick = {
                        settings.updateIsScanModeInclusive(true)
                    }
                ),
                SettingSegmentOption(
                    icon = Icons.Rounded.Remove,
                    contentDescription = context.resources.getString(R.string.exclusive),
                    onClick = {
                        settings.updateIsScanModeInclusive(false)
                    }
                )
            )
        }
        SettingSegmentOptions(
            title = context.resources.getString(R.string.scan_mode),
            supportingText = context.resources.getString(R.string.scan_mode_explain),
            icon = Icons.Rounded.LocationSearching,
            options = scanModeOptions,
            selectedOptionIndex = if (isScanModeInclusive) 0 else 1,
        )

        AnimatedContent(
            targetState = isScanModeInclusive,
            label = "scan-settings-animation",
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }
        ) { state ->
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (state) {
                    true -> {
                        val isScanMusicFolderChecked by settings.scanMusicFolder.collectAsState()
                        SettingSwitch(
                            title = context.resources.getString(R.string.scan_music_folder),
                            supportingText = context.resources.getString(R.string.scan_music_folder_explain),
                            icon = Icons.Rounded.LibraryMusic,
                            isChecked = isScanMusicFolderChecked,
                            onCheckedChange = {
                                settings.updateScanMusicFolder(it)
                            }
                        )

                        val includedScanFolders by settings.extraScanFolders.collectAsState()
                        SettingFoldersPicked(
                            title = context.resources.getString(R.string.extra_scan_folders),
                            paths = includedScanFolders.toList(),
                            addFolderContentDescription = context.resources.getString(R.string.pick_folder_to_include_in_scanning),
                            removeFolderContentDescription = context.resources.getString(R.string.remove_folder_from_included_in_scanning),
                            onPickFolderClick = {
                                onFolderPick()
                            },
                            onRemoveFolderClick = {
                                settings.updateExtraScanFolders(
                                    includedScanFolders - it
                                )
                            },
                            availableOptionsTitle = context.resources.getString(R.string.folders_with_audio),
                            availableOptions = foldersWithAudio.toList(),
                            addFolderFromOptionsContentDescription = context.resources.getString(
                                R.string.include_folder_in_scanning
                            ),
                            onAddFolderFromOptionsClick = {
                                settings.updateExtraScanFolders(
                                    includedScanFolders + it
                                )
                            },
                            onScanClick = onScanFoldersClick,
                            scanContentDescription = context.resources.getString(R.string.scan_for_folders_with_audio),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    false -> {
                        val excludedScanFolders by settings.excludedScanFolders.collectAsState()
                        SettingFoldersPicked(
                            title = context.resources.getString(R.string.excluded_folders),
                            paths = excludedScanFolders.toList(),
                            addFolderContentDescription = context.resources.getString(R.string.pick_folder_to_exclude_from_scanning),
                            removeFolderContentDescription = context.resources.getString(R.string.remove_folder_from_excluded_from_scanning),
                            onPickFolderClick = {
                                onFolderPick()
                            },
                            onRemoveFolderClick = {
                                settings.updateExcludedScanFolders(
                                    excludedScanFolders - it
                                )
                            },
                            availableOptionsTitle = context.resources.getString(R.string.folders_with_audio),
                            availableOptions = foldersWithAudio.toList(),
                            addFolderFromOptionsContentDescription = context.resources.getString(
                                R.string.exclude_folder_from_scanning
                            ),
                            onAddFolderFromOptionsClick = {
                                settings.updateExcludedScanFolders(
                                    excludedScanFolders + it
                                )
                            },
                            onScanClick = onScanFoldersClick,
                            scanContentDescription = context.resources.getString(R.string.scan_for_folders_with_audio),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}