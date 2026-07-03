package com.yunfie.illustia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yunfie.illustia.IllustiaViewModel
import com.yunfie.illustia.R
import com.yunfie.illustia.data.pixiv.IllustSeriesStore
import com.yunfie.illustia.models.Illust
import com.yunfie.illustia.models.pixiv.Illusts
import com.yunfie.illustia.ui.components.ElevatedPanel
import com.yunfie.illustia.ui.components.EmptyState
import com.yunfie.illustia.ui.components.IllustCard
import com.yunfie.illustia.ui.components.IllustCardSkeleton
import com.yunfie.illustia.ui.components.PixivImage
import com.yunfie.illustia.ui.components.PrefetchPixivImages
import com.yunfie.illustia.ui.components.adaptiveIllustColumns
import com.yunfie.illustia.ui.components.overlayActionButtonColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun IllustSeriesScreen(
    seriesId: Long,
    viewModel: IllustiaViewModel,
    onBack: () -> Unit,
    onOpenIllust: (Long) -> Unit,
) {
    val repository = remember(viewModel) { viewModel.uiRepository() }
    val store = remember(repository, seriesId) { IllustSeriesStore(repository, seriesId) }
    val state by store.state.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val feedHighQuality = remember(settings.highQualityImages, settings.feedPreviewQuality) {
        settings.highQualityImages && settings.feedPreviewQuality != "low"
    }
    val showAiBadge = remember(settings.showAiBadge) { settings.showAiBadge }
    val prefetchUrls = remember(state.illusts, feedHighQuality) {
        state.illusts.asSequence()
            .take(16)
            .map { if (feedHighQuality) it.imageUrls.medium.ifBlank { it.imageUrls.large } else it.imageUrls.squareMedium }
            .toList()
    }

    PrefetchPixivImages(prefetchUrls, enabled = settings.prefetchImages)

    LaunchedEffect(store) {
        store.fetch()
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = state.model?.illustSeriesDetail?.title ?: stringResource(R.string.detail_series),
                largeTitle = state.model?.illustSeriesDetail?.title ?: stringResource(R.string.detail_series),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { store.fetch() } }) {
                        Icon(MiuixIcons.Refresh, contentDescription = stringResource(R.string.action_load_more))
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        PullToRefresh(
            isRefreshing = state.isLoading && state.illusts.isNotEmpty(),
            onRefresh = { scope.launch { store.fetch() } },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(adaptiveIllustColumns(settings)),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = scaffoldPadding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SeriesHeader(
                        detailTitle = state.model?.illustSeriesDetail?.title.orEmpty(),
                        coverUrl = state.model?.illustSeriesDetail?.coverImageUrls?.medium,
                        caption = state.model?.illustSeriesDetail?.caption.orEmpty(),
                        watchlistAdded = state.watchlistAdded,
                        onToggleWatchlist = {
                            scope.launch {
                                if (state.watchlistAdded) store.removeWatchlist() else store.addWatchlist()
                            }
                        },
                    )
                }
                if (state.isLoading && state.illusts.isEmpty()) {
                    items(6, contentType = { "illust_skeleton" }) { IllustCardSkeleton() }
                }
                if (state.errorMessage != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text(state.errorMessage ?: "", color = MiuixTheme.colorScheme.error) }
                }
                if (state.illusts.isEmpty() && !state.isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyState(stringResource(R.string.detail_related)) }
                }
                gridItems(state.illusts, key = { it.id }, contentType = { "illust_card" }) { illust ->
                    val illustId = illust.id
                    val cardIllust = remember(illustId) { illust.toIllust() }
                    val onBookmark = remember(illustId) { { viewModel.toggleBookmark(illustId, cardIllust) } }
                    val onClick = remember(illustId) { { onOpenIllust(illustId) } }
                    val onLongClick = remember(illustId) { { viewModel.onIllustLongPress(illustId, cardIllust) } }

                    IllustCard(
                        illust = cardIllust,
                        onBookmark = onBookmark,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        highQualityImages = feedHighQuality,
                        showAiBadge = showAiBadge,
                    )
                }
                if (state.model?.nextUrl != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Button(
                            onClick = { scope.launch { store.loadMore() } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = overlayActionButtonColors(),
                        ) {
                            Text(stringResource(R.string.action_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesHeader(
    detailTitle: String,
    coverUrl: String?,
    caption: String,
    watchlistAdded: Boolean,
    onToggleWatchlist: () -> Unit,
) {
    ElevatedPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                ) {
                    if (!coverUrl.isNullOrBlank()) {
                        PixivImage(
                            url = coverUrl,
                            contentDescription = detailTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            thumbnail = true,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = detailTitle.ifBlank { stringResource(R.string.detail_series) },
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Button(
                        onClick = onToggleWatchlist,
                        colors = overlayActionButtonColors(),
                    ) {
                        Text(if (watchlistAdded) stringResource(R.string.action_remove_bookmark) else stringResource(R.string.action_add))
                    }
                }
            }
            if (caption.isNotBlank()) {
                Text(
                    text = caption,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}

private fun Illusts.toIllust(): Illust {
    return Illust(
        id = id,
        title = title,
        type = type,
        caption = caption,
        artistId = user.id,
        artistName = user.name,
        artistAvatarUrl = user.profileImageUrls.medium,
        squareImageUrl = imageUrls.squareMedium,
        mediumImageUrl = imageUrls.medium,
        imageUrl = imageUrls.large,
        originalImageUrl = metaSinglePage?.originalImageUrl,
        tags = tags.map { it.name },
        pageCount = pageCount,
        isBookmarked = isBookmarked,
        totalComments = totalComments,
        series = series,
    )
}
