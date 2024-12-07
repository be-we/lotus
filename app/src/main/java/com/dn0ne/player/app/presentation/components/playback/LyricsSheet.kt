package com.dn0ne.player.app.presentation.components.playback

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.LazyColumnWithCollapsibleTopBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun LyricsSheet(
    playbackStateFlow: StateFlow<PlaybackState>,
    onBackClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }
    val playbackState by playbackStateFlow.collectAsState()
    val isLoadingLyrics by remember {
        derivedStateOf {
            playbackState.isLoadingLyrics
        }
    }
    val lyrics by remember {
        derivedStateOf {
            playbackState.lyrics
        }
    }
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }
    val context = LocalContext.current

    var showSyncedLyrics by remember(lyrics) {
        mutableStateOf<Boolean?>(
            when {
                lyrics?.synced != null -> true
                lyrics?.plain != null -> false
                else -> null
            }
        )
    }

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.inverseSurface
    ) {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        if (showSyncedLyrics == true) {
            LaunchedEffect(Unit) {
                val index = lyrics?.synced?.indexOfFirst { playbackState.position < it.first } ?: -1
                if (index >= 0) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            index = index + 1,
                            scrollOffset = -listState.layoutInfo.viewportSize.height / 3
                        )
                    }
                }
            }
        }

        LazyColumnWithCollapsibleTopBar(
            topBarContent = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = context.resources.getString(R.string.close_lyrics_sheet)
                    )
                }

                Text(
                    text = context.resources.getString(R.string.lyrics),
                    fontSize = lerp(
                        MaterialTheme.typography.titleLarge.fontSize,
                        MaterialTheme.typography.displaySmall.fontSize,
                        collapseFraction
                    ),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )

                if (lyrics?.synced != null && lyrics?.plain != null && showSyncedLyrics != null) {
                    LyricsTypeSwitch(
                        isSynced = showSyncedLyrics!!,
                        onIsSyncedSwitch = {
                            showSyncedLyrics = it
                        },
                        enabled = collapseFraction == 1f,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .alpha(2 * (collapseFraction - 0.5f))
                    )
                }
            },
            collapseFraction = {
                collapseFraction = it
            },
            listState = listState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.inversePrimary)
                .clickable(enabled = false, onClick = {})
                .safeDrawingPadding()
        ) {
            when (showSyncedLyrics) {
                null -> {
                    item(key = isLoadingLyrics) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isLoadingLyrics) {
                                Text(
                                    text = context.resources.getString(R.string.loading_lyrics),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .5f),
                                    modifier = Modifier.width(100.dp)
                                )
                            } else {
                                Text(text = context.resources.getString(R.string.cant_find_lyrics))
                            }
                        }
                    }
                }
                true -> {
                    lyrics?.synced?.let { synced ->
                        itemsIndexed(
                            items = synced,
                            key = { index, (time, _) -> "$index-$time"}
                        ) { index, (time, line) ->
                            Column {
                                val nextTime = remember {
                                    synced.getOrNull(index + 1)?.first ?: Int.MAX_VALUE
                                }

                                SyncedLyricsLine(
                                    positionFlow = playbackStateFlow.map { it.position },
                                    time = time,
                                    nextTime = nextTime,
                                    line = line,
                                    onClick = {
                                        onSeekTo(time.toLong())
                                    },
                                    onBecomeCurrent = { textHeight ->
                                        val isItemVisible = listState.layoutInfo
                                            .visibleItemsInfo
                                            .find { it.index == index } != null

                                        if (isItemVisible && !listState.isScrollInProgress) {
                                            val offsetToCenterText =
                                                textHeight.toInt() -
                                                        listState.layoutInfo.viewportSize.height / 2

                                            coroutineScope.launch {
                                                listState.animateScrollToItem(
                                                    index = index + 1,
                                                    scrollOffset = offsetToCenterText
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                false -> {
                    lyrics?.plain?.let { plain ->

                        itemsIndexed(
                            items = plain,
                            key = { index, line -> "$index-$line"}
                        ) { index, line ->
                            Column {
                                PlainLyricsLine(
                                    line = line,
                                    modifier = Modifier.animateItem()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/*@Composable
fun LyricsSheet(
    playbackStateFlow: StateFlow<PlaybackState>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }
    val playbackState by playbackStateFlow.collectAsState()
    val isLoadingLyrics by remember {
        derivedStateOf {
            playbackState.isLoadingLyrics
        }
    }
    val lyrics by remember {
        derivedStateOf {
            playbackState.lyrics
        }
    }
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }
    val context = LocalContext.current

    var showSyncedLyrics by remember(lyrics) {
        mutableStateOf<Boolean?>(
            when {
                lyrics?.synced != null -> true
                lyrics?.plain != null -> false
                else -> null
            }
        )
    }

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.inverseSurface
    ) {
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
                        contentDescription = context.resources.getString(R.string.close_lyrics_sheet)
                    )
                }

                Text(
                    text = context.resources.getString(R.string.lyrics),
                    fontSize = lerp(
                        MaterialTheme.typography.titleLarge.fontSize,
                        MaterialTheme.typography.displaySmall.fontSize,
                        collapseFraction
                    ),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )

                if (lyrics?.synced != null && lyrics?.plain != null && showSyncedLyrics != null) {
                    LyricsTypeSwitch(
                        isSynced = showSyncedLyrics!!,
                        onIsSyncedSwitch = {
                            showSyncedLyrics = it
                        },
                        enabled = collapseFraction == 1f,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .alpha(2 * (collapseFraction - 0.5f))
                    )
                }
            },
            collapseFraction = {
                collapseFraction = it
            },
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.inversePrimary)
                .clickable(enabled = false, onClick = {})
                .safeDrawingPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = showSyncedLyrics,
                label = "lyrics-type-change-animation",
                modifier = Modifier.fillMaxSize()
            ) { showSyncedLyrics ->
                when {
                    showSyncedLyrics == null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isLoadingLyrics) {
                                Text(
                                    text = context.resources.getString(R.string.loading_lyrics),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .5f),
                                    modifier = Modifier.width(100.dp)
                                )
                            } else {
                                Text(text = context.resources.getString(R.string.cant_find_lyrics))
                            }
                        }
                    }

                    showSyncedLyrics -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            lyrics?.synced?.forEachIndexed { index, (time, line) ->
                                val nextTime = remember {
                                    lyrics?.synced?.getOrNull(index + 1)?.first ?: Int.MAX_VALUE
                                }

                                SyncedLyricsLine(
                                    positionFlow = playbackStateFlow.map { it.position },
                                    time = time,
                                    nextTime = nextTime,
                                    line = line
                                )
                            }
                        }
                    }

                    !showSyncedLyrics -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            lyrics?.plain?.forEach { line ->
                                PlainLyricsLine(
                                    line = line,
                                    color = LocalContentColor.current
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}*/

@Composable
fun LyricsTypeSwitch(
    isSynced: Boolean,
    onIsSyncedSwitch: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min)
            .clip(ShapeDefaults.ExtraLarge)
            .background(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
    ) {
        var midPoint by remember {
            mutableStateOf(0.dp)
        }
        val density = LocalDensity.current
        val capsuleOffset by animateDpAsState(
            targetValue = if (isSynced) 0.dp else midPoint,
            label = "capsule-offset-animation"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(.5f)
                .offset(x = capsuleOffset)
                .clip(ShapeDefaults.ExtraLarge)
                .background(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .onGloballyPositioned {
                    midPoint = with(density) { (it.size.width / 2).toDp() }
                }
        ) {
            Text(
                text = context.resources.getString(R.string.synced),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapeDefaults.ExtraLarge)
                    .clickable(
                        enabled = !isSynced && enabled,
                        indication = null,
                        interactionSource = remember {
                            MutableInteractionSource()
                        }
                    ) {
                        onIsSyncedSwitch(true)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Text(
                text = context.resources.getString(R.string.plain),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapeDefaults.ExtraLarge)
                    .clickable(
                        enabled = isSynced && enabled,
                        indication = null,
                        interactionSource = remember {
                            MutableInteractionSource()
                        }
                    ) {
                        onIsSyncedSwitch(false)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun PlainLyricsLine(
    line: String,
    color: Color = LocalContentColor.current,
    modifier: Modifier = Modifier
) {
    Text(
        text = line,
        style = MaterialTheme.typography.headlineMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
fun SyncedLyricsLine(
    positionFlow: Flow<Long>,
    time: Int,
    nextTime: Int,
    line: String,
    onClick: () -> Unit,
    onBecomeCurrent: (textHeight: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val position by positionFlow.collectAsState(0)
    var textHeight by remember {
        mutableFloatStateOf(0f)
    }
    val isCurrentLine by remember {
        derivedStateOf {
            position in time..nextTime
        }
    }

    LaunchedEffect(isCurrentLine) {
        if (isCurrentLine) {
            onBecomeCurrent(textHeight)
        }
    }

    val progressFraction by remember {
        derivedStateOf {
            ((position.toFloat() - time) / (nextTime - time))
                .coerceIn(0f, 1f)
        }
    }

    val gradientOrigin by remember {
        derivedStateOf {
            progressFraction * textHeight
        }
    }

    val localContentColor = LocalContentColor.current

    val color by animateColorAsState(
        targetValue = if (isCurrentLine) {
            localContentColor
        } else localContentColor.copy(alpha = .5f),
        label = "lyrics-color-animation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isCurrentLine) 1.05f else 1f,
        label = "current-line-scale-animation"
    )

    Text(
        text = line,
        style = MaterialTheme.typography.headlineMedium
            .copy(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = color.alpha / 2)
                    ),
                    startY = gradientOrigin - 10f,
                    endY = gradientOrigin + 10f
                ),
                shadow = Shadow(
                    color = localContentColor.copy(alpha = .5f),
                    blurRadius = if (isCurrentLine) progressFraction * 20f else 0f
                )
            ),
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .onGloballyPositioned {
                textHeight = it.size.height.toFloat()
            }
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, .5f)
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}