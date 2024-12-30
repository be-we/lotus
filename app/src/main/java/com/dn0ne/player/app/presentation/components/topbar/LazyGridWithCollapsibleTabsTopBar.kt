package com.dn0ne.player.app.presentation.components.topbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.dn0ne.player.app.presentation.components.animatable.rememberAnimatable
import com.dn0ne.player.app.presentation.components.isSystemInLandscapeOrientation
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LazyGridWithCollapsibleTabsTopBar(
    topBarTabTitles: List<String>,
    defaultSelectedTabIndex: Int = 0,
    tabTitleTextStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    tabRowTitleTextStyle: TextStyle = MaterialTheme.typography.titleLarge,
    topBarButtons: @Composable BoxScope.(tabIndex: Int) -> Unit = {},
    minTopBarHeight: Dp = 60.dp,
    maxTopBarHeight: Dp = 250.dp,
    maxTopBarHeightLandscape: Dp = 150.dp,
    collapsedByDefault: Boolean = false,
    collapseFraction: (Float) -> Unit = {},
    listState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentHorizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    contentVerticalArrangement: Arrangement.Vertical = Arrangement.Top,
    modifier: Modifier = Modifier,
    gridCells: (tabIndex: Int) -> GridCells = { GridCells.Fixed(1) },
    tabContent: LazyGridScope.(tabIndex: Int) -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val isInLandscapeOrientation = isSystemInLandscapeOrientation()
    val minTopBarHeight = remember { with(density) { minTopBarHeight.toPx() } }
    val maxTopBarHeight = remember {
        with(density) {
            if (isInLandscapeOrientation) {
                maxTopBarHeightLandscape.toPx()
            } else maxTopBarHeight.toPx()
        }
    }
    val topBarHeight = rememberAnimatable(
        initialValue = if (collapsedByDefault || isInLandscapeOrientation) {
            minTopBarHeight
        } else maxTopBarHeight
    )

    LaunchedEffect(isInLandscapeOrientation) {
        topBarHeight.snapTo(maxTopBarHeight)
    }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction(
            (topBarHeight.value - minTopBarHeight) / (maxTopBarHeight - minTopBarHeight)
        )
    }

    val topBarScrollConnection = remember {
        return@remember object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val previousHeight = topBarHeight.value
                val newHeight = if (listState.firstVisibleItemIndex >= 0 && available.y < 0) {
                    (previousHeight + available.y).coerceIn(
                        minTopBarHeight,
                        maxTopBarHeight
                    )
                } else if (listState.firstVisibleItemIndex == 0) {
                    (previousHeight + available.y).coerceIn(
                        minTopBarHeight,
                        maxTopBarHeight
                    )
                } else previousHeight

                coroutineScope.launch {
                    topBarHeight.snapTo(newHeight)
                }
                return Offset(0f, newHeight - previousHeight)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                coroutineScope.launch {
                    val threshold = (maxTopBarHeight - minTopBarHeight)
                    topBarHeight.animateTo(
                        targetValue = if (topBarHeight.value < threshold) minTopBarHeight else maxTopBarHeight,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                }

                return super.onPostFling(consumed, available)
            }
        }
    }

    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(defaultSelectedTabIndex.coerceIn(topBarTabTitles.indices))
    }
    var showTabRow by remember {
        mutableStateOf(false)
    }
    val isColumnScrollInProgress by remember {
        derivedStateOf {
            listState.isScrollInProgress
        }
    }

    LaunchedEffect(isColumnScrollInProgress) {
        if (showTabRow && isColumnScrollInProgress) {
            showTabRow = false
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(topBarScrollConnection)
    ) {
        Column {
            Spacer(
                modifier = Modifier
                    .height(with(density) { topBarHeight.value.toDp() })
            )
            AnimatedContent(
                targetState = selectedTabIndex,
                label = "column-tab-animation",
            ) { tabIndex ->
                if (tabIndex != defaultSelectedTabIndex) {
                    BackHandler {
                        selectedTabIndex = defaultSelectedTabIndex
                    }
                }

                LazyVerticalGrid(
                    columns = gridCells(tabIndex),
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    horizontalArrangement = contentHorizontalArrangement,
                    verticalArrangement = contentVerticalArrangement
                ) {
                    item(
                        span = {
                            GridItemSpan(this.maxLineSpan)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                        )
                    }

                    tabContent(tabIndex)

                    item(
                        span = {
                            GridItemSpan(this.maxLineSpan)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { topBarHeight.value.toDp() })
        ) {
            val tabListState = rememberLazyListState()
            val boundTransformAnimationSpec = remember { spring<Rect>() }
            val contentAnimationSpec = remember { spring<Float>() }

            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                SharedTransitionLayout {
                    AnimatedContent(
                        targetState = showTabRow,
                        transitionSpec = {
                            scaleIn(contentAnimationSpec, initialScale = 1.5f) +
                                    fadeIn(contentAnimationSpec) togetherWith
                                    scaleOut(contentAnimationSpec, targetScale = 1.5f) +
                                    fadeOut(contentAnimationSpec)
                        },
                        label = "top-bar-title-animation"
                    ) { state ->
                        when (state) {
                            false -> {
                                val viewportWidth by remember {
                                    derivedStateOf {
                                        tabListState.layoutInfo.viewportSize.width
                                    }
                                }
                                val listItemsCount by remember {
                                    derivedStateOf {
                                        tabListState.layoutInfo.totalItemsCount
                                    }
                                }

                                LaunchedEffect(listItemsCount) {
                                    tabListState.scrollToItem(
                                        index = selectedTabIndex + 1,
                                    )

                                    tabListState.scrollToItem(
                                        index = selectedTabIndex + 1,
                                        scrollOffset = -viewportWidth / 2 + tabListState.layoutInfo.visibleItemsInfo.first { it.index == selectedTabIndex + 1 }.size / 2
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = null
                                        ) {
                                            showTabRow = true
                                        }
                                ) {
                                    TabTitle(
                                        selectedTabIndex = selectedTabIndex,
                                        title = topBarTabTitles[selectedTabIndex],
                                        style = tabTitleTextStyle,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundTransformAnimationSpec = boundTransformAnimationSpec,
                                        modifier = Modifier.align(Alignment.Center)
                                    )

                                    topBarButtons(selectedTabIndex)
                                }
                            }

                            true -> {
                                val viewportWidth by remember {
                                    derivedStateOf {
                                        tabListState.layoutInfo.viewportSize.width
                                    }
                                }

                                LazyRow(
                                    state = tabListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = null
                                        ) {
                                            showTabRow = false
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item {
                                        Spacer(
                                            modifier = Modifier.width(
                                                with(density) {
                                                    (viewportWidth / 2.5f).toDp()
                                                }
                                            )
                                        )
                                    }

                                    itemsIndexed(
                                        items = topBarTabTitles,
                                        key = { index, title -> "$index-$title" }
                                    ) { index, title ->
                                        TabRowTitle(
                                            selectedTabIndex = selectedTabIndex,
                                            index = index,
                                            title = title,
                                            style = tabRowTitleTextStyle,
                                            onClick = {
                                                selectedTabIndex = index
                                                showTabRow = false
                                            },
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundTransformAnimationSpec = boundTransformAnimationSpec
                                        )
                                    }

                                    item {
                                        Spacer(
                                            modifier = Modifier.width(
                                                with(density) {
                                                    (viewportWidth / 2.5f).toDp()
                                                }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TabTitle(
    selectedTabIndex: Int,
    title: String,
    style: TextStyle,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundTransformAnimationSpec: FiniteAnimationSpec<Rect>,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Min)
        ) {
            val title by remember(selectedTabIndex) {
                mutableStateOf(title)
            }
            Text(
                text = title,
                style = style,
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(title),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            boundTransformAnimationSpec
                        }
                    )
            )

            Box(
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            "indicator"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        boundsTransform = { _, _ ->
                            boundTransformAnimationSpec
                        }
                    )
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(ShapeDefaults.ExtraLarge)
                    .background(
                        color = MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TabRowTitle(
    selectedTabIndex: Int,
    index: Int,
    title: String,
    style: TextStyle,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundTransformAnimationSpec: FiniteAnimationSpec<Rect>,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .heightIn(min = 60.dp)
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null
                ) {
                    onClick()
                }
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            key("$selectedTabIndex-$index") {
                Text(
                    text = title,
                    style = style,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(title),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                boundTransformAnimationSpec
                            }
                        )
                )

                val primary = MaterialTheme.colorScheme.primary
                val color by remember {
                    mutableStateOf(
                        if (selectedTabIndex == index) {
                            primary
                        } else Color.Transparent
                    )
                }
                Box(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                if (color != Color.Transparent) {
                                    "indicator"
                                } else {
                                    "invisible-indicator"
                                }
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                            boundsTransform = { _, _ ->
                                boundTransformAnimationSpec
                            }
                        )
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(ShapeDefaults.ExtraLarge)
                        .background(
                            color = color
                        )
                )
            }
        }
    }
}