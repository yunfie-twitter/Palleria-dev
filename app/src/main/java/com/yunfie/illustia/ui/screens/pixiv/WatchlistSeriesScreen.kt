package com.yunfie.illustia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yunfie.illustia.IllustiaViewModel
import com.yunfie.illustia.R
import com.yunfie.illustia.data.pixiv.WatchlistStore
import com.yunfie.illustia.models.pixiv.MangaSeriesModel
import com.yunfie.illustia.ui.components.ElevatedPanel
import com.yunfie.illustia.ui.components.EmptyState
import com.yunfie.illustia.ui.components.LoadingIndicator
import com.yunfie.illustia.ui.components.miuixClickable
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
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WatchlistSeriesScreen(
    viewModel: IllustiaViewModel,
    onBack: () -> Unit,
    onOpenSeries: (Long) -> Unit,
) {
    val repository = remember(viewModel) { viewModel.uiRepository() }
    val store = remember(repository) { WatchlistStore(repository) }
    val state by store.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(store) {
        store.fetch()
    }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.watchlist_series_title),
                largeTitle = stringResource(R.string.watchlist_series_title),
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
            isRefreshing = state.isLoading && state.mangaSeries.isNotEmpty(),
            onRefresh = { scope.launch { store.fetch() } },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = scaffoldPadding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.errorMessage != null) {
                    item { Text(state.errorMessage ?: "", color = MiuixTheme.colorScheme.error) }
                }
                if (state.mangaSeries.isEmpty() && !state.isLoading) {
                    item { EmptyState(stringResource(R.string.watchlist_series_empty)) }
                }
                items(state.mangaSeries, key = { it.id }) { series ->
                    WatchlistSeriesRow(
                        series = series,
                        onClick = { onOpenSeries(series.id) },
                    )
                }
                if (state.model?.nextUrl != null) {
                    item {
                        Button(
                            onClick = { scope.launch { store.loadMore() } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = overlayActionButtonColors(),
                        ) {
                            Text(stringResource(R.string.watchlist_series_load_more))
                        }
                    }
                }
                if (state.isLoading && state.mangaSeries.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistSeriesRow(
    series: MangaSeriesModel,
    onClick: () -> Unit,
) {
    ElevatedPanel(
        modifier = Modifier
            .fillMaxWidth()
            .miuixClickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.FavoritesFill,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = series.title,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = series.user?.name?.takeIf { it.isNotBlank() } ?: "@${series.user?.account.orEmpty()}",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "ID ${series.id} ・ ${series.publishedContentCount}P",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote2,
                )
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
