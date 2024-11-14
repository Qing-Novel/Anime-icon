package com.sakura.anime.presentation.screen.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sakura.anime.R
import com.sakura.anime.domain.model.Anime
import com.sakura.anime.domain.model.Home
import com.sakura.anime.presentation.component.LoadingIndicator
import com.sakura.anime.presentation.component.MediaSmall
import com.sakura.anime.presentation.component.MediaSmallRow
import com.sakura.anime.presentation.component.StateHandler
import com.sakura.anime.presentation.component.TranslucentStatusBarLayout
import com.sakura.anime.presentation.component.WarningMessage
import com.sakura.anime.util.KEY_HOME_BACKGROUND_URI
import com.sakura.anime.util.KEY_USE_GRID_LAYOUT
import com.sakura.anime.util.SourceHolder
import com.sakura.anime.util.SourceMode
import com.sakura.anime.util.bannerParallax
import com.sakura.anime.util.isWideScreen
import com.sakura.anime.util.rememberPreference
import kotlinx.coroutines.launch
import com.sakura.anime.R as Res

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAnimeDetail: (detailUrl: String, mode: SourceMode) -> Unit,
) {
    val homeViewModel = hiltViewModel<HomeViewModel>()
    val availableDataList = homeViewModel.homeDataList.collectAsState()

    LaunchedEffect(SourceHolder.isSourceChanged) {
        if (SourceHolder.isSourceChanged) {
            homeViewModel.refresh()
            SourceHolder.isSourceChanged = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        StateHandler(
            state = availableDataList.value,
            onLoading = { LoadingIndicator() },
            onFailure = {
                WarningMessage(
                    textId = Res.string.txt_empty_result,
                    onRetryClick = { homeViewModel.refresh() }
                )
            }
        ) { resource ->

            val scrollState = rememberScrollState()
            var useGridLayout by rememberPreference(KEY_USE_GRID_LAYOUT, false)

            TranslucentStatusBarLayout(
                scrollState = scrollState,
            ) {
                Box(modifier = Modifier.run {
                    if (useGridLayout) this else verticalScroll(scrollState)
                }) {
                    val context = LocalContext.current
                    val isWideScreen = isWideScreen(context)

                    if (!isWideScreen) {
                        HomeBackground(
                            scrollState = scrollState,
                            useGridLayout = useGridLayout,
                            onSwitchLayout = { useGridLayout = it }
                        )
                    }

                    resource.data?.let { data ->
                        HomeContent(
                            data = data,
                            useGridLayout = useGridLayout,
                            onSwitchGridLayout = { useGridLayout = it },
                            isWideScreen = isWideScreen,
                            onItemClick = {
                                onNavigateToAnimeDetail(
                                    it.detailUrl,
                                    SourceHolder.currentSourceMode
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    data: List<Home>,
    useGridLayout: Boolean,
    onSwitchGridLayout: (Boolean) -> Unit,
    isWideScreen: Boolean,
    onItemClick: (Anime) -> Unit
) {
    Column {
        HomeBackgroundSpacer(isWideScreen, useGridLayout)

        val padding = if (useGridLayout) 0.dp else dimensionResource(Res.dimen.medium_padding)
        val homeBackgroundColor = getBlendedBackgroundColor()

        Column(
            modifier = Modifier
                .background(if (!isWideScreen) homeBackgroundColor else MaterialTheme.colorScheme.background)
                .padding(vertical = padding),
            verticalArrangement = Arrangement.spacedBy(padding),
        ) {
            if (isWideScreen) {
                HomeTile(
                    useGridLayout = useGridLayout,
                    onSwitchGridLayout = onSwitchGridLayout
                )
            }

            if (useGridLayout) {
                GridLayoutTabs(
                    data = data,
                    onItemClick = onItemClick
                )
            } else {
                data.forEach { home ->
                    HomeRow(
                        list = home.animeList,
                        title = home.title,
                        onItemClicked = onItemClick
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridLayoutTabs(
    data: List<Home>,
    onItemClick: (Anime) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { data.size })
    val scope = rememberCoroutineScope()
    PrimaryScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = Color.Unspecified,
        edgePadding = 0.dp,
        indicator = {},
        divider = {}
    ) {
        val tabs = remember(data) { data.map { it.title } }
        tabs.forEachIndexed { index, title ->
            Tab(
                text = { title },
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch { pagerState.scrollToPage(index) }
                },
            )
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        val context = LocalContext.current
        val isWideScreen = isWideScreen(context)
        // 判断使用的列数和布局宽度
        val columns = if (isWideScreen) {
            GridCells.Adaptive(minSize = dimensionResource(R.dimen.min_media_card_width))
        } else {
            GridCells.Fixed(3)
        }
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = columns,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            items(data[page].animeList) { homeItem ->
                MediaSmall(
                    image = homeItem.img,
                    label = homeItem.title,
                    onClick = { onItemClick(homeItem) },
                    modifier = Modifier.width(dimensionResource(Res.dimen.media_card_width))
                )
            }
        }
    }
}

@Composable
fun getBlendedBackgroundColor(): Color {
    return Color(
        ColorUtils.blendARGB(
            MaterialTheme.colorScheme.background.toArgb(),
            MaterialTheme.colorScheme.primaryContainer.toArgb(),
            0.05f
        )
    )
}

@Composable
private fun HomeBackgroundSpacer(isWideScreen: Boolean, useGridLayout: Boolean) {
    val size by animateDpAsState(
        targetValue = if (!isWideScreen) {
            if (useGridLayout)
                dimensionResource(Res.dimen.banner_height) - 56.dp
            else
                dimensionResource(Res.dimen.banner_height)
        } else 0.dp,
        animationSpec = tween(400),
        label = "background_padding"
    )
    Spacer(Modifier.size(size))
}

@Composable
private fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> String,
) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(11.dp)
            )
            .clip(RoundedCornerShape(11.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text(),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun HomeBackground(
    scrollState: ScrollState,
    useGridLayout: Boolean,
    onSwitchLayout: (Boolean) -> Unit,
) {
    Box {
        val context = LocalContext.current
        var imageUri by rememberPreference(key = KEY_HOME_BACKGROUND_URI, defaultValue = "")

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) {
            it?.let { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                imageUri = uri.toString()
            }
        }

        AsyncImage(
            model = Uri.parse(imageUri),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    if (useGridLayout)
                        dimensionResource(Res.dimen.banner_height) - 56.dp
                    else
                        dimensionResource(Res.dimen.banner_height)
                )
                .bannerParallax(scrollState),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            error = painterResource(Res.drawable.background)
        )

        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    )
                )
                .fillMaxWidth()
                .height(dimensionResource(Res.dimen.banner_height))
        )

        val offsetY by animateDpAsState(
            targetValue = if (useGridLayout) (-56).dp else 0.dp,
            animationSpec = tween(400),
            label = "hometile_offset"
        )
        HomeTile(
            modifier = Modifier
                .offset(y = offsetY)
                .align(Alignment.BottomStart),
            useGridLayout = useGridLayout,
            onSwitchGridLayout = onSwitchLayout
        ) {
            launcher.launch(arrayOf("image/*"))
        }
    }
}

@Composable
private fun HomeTile(
    modifier: Modifier = Modifier,
    useGridLayout: Boolean,
    onSwitchGridLayout: (Boolean) -> Unit,
    onClick: () -> Unit = {},
) {
    val isWideScreen = isWideScreen(LocalContext.current)
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(
                    start = dimensionResource(Res.dimen.large_padding),
                    bottom = if (!isWideScreen) dimensionResource(Res.dimen.medium_padding) else 0.dp
                )
        ) {
            if (!isWideScreen) {
                Text(
                    text = stringResource(Res.string.lbl_anime),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.clickable { onClick() }
                )
            }
            Text(
                text = SourceHolder.currentSourceMode.name,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = 8.dp)
                    .padding(vertical = if (isWideScreen && useGridLayout) 8.dp else 0.dp)
                    .run {
                        if (isWideScreen) {
                            // 获取焦点
                            clickable { }
                        } else this
                    }
            )
        }

        if (!isWideScreen) {
            LayoutTypeSelector(
                modifier = Modifier
                    .padding(end = 24.dp, bottom = 16.dp),
                checked = useGridLayout,
                onCheckedChange = { onSwitchGridLayout(it) }
            )
        }
    }
}

@Composable
fun HomeRow(
    list: List<Anime?>,
    title: String,
    onItemClicked: (Anime) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = dimensionResource(Res.dimen.large_padding))
        )

        Spacer(Modifier.size(dimensionResource(Res.dimen.medium_padding)))

        MediaSmallRow(
            mediaList = list,
            content = { homeItem ->
                MediaSmall(
                    image = homeItem?.img,
                    label = homeItem?.title,
                    onClick = {
                        onItemClicked(homeItem!!)
                    },
                    modifier = Modifier.width(dimensionResource(Res.dimen.media_card_width))
                )
            }
        )
    }
}

@Composable
private fun LayoutTypeSelector(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onBackground,
                shape = CircleShape
            )
    ) {
        val offset by animateDpAsState(
            targetValue = if (!checked) 0.dp else 40.dp,
            label = "media_switch"
        )

        Box(
            modifier = Modifier
                .padding(dimensionResource(Res.dimen.media_type_selector_padding))
                .size(dimensionResource(Res.dimen.media_type_choice_size))
                .offset { IntOffset(x = offset.roundToPx(), y = 0) }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        )

        Row(
            modifier = Modifier
                .height(dimensionResource(Res.dimen.media_type_selector_height))
                .width(dimensionResource(Res.dimen.media_type_selector_width))
                .padding(dimensionResource(Res.dimen.media_type_selector_padding)),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                val selectedIndex = if (!checked) 0 else 1
                IconButton(
                    onClick = { if (selectedIndex != index) onCheckedChange?.invoke(!checked) },
                    modifier = Modifier.requiredWidth(dimensionResource(Res.dimen.media_type_choice_size))
                ) {
                    Icon(
                        imageVector = if (index == 0) Icons.Rounded.PlayArrow else ImageVector.vectorResource(
                            id = Res.drawable.manga
                        ),
                        tint = animateColorAsState(
                            targetValue = if (selectedIndex == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.background,
                            animationSpec = tween(400),
                            label = "icon_color"
                        ).value,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
