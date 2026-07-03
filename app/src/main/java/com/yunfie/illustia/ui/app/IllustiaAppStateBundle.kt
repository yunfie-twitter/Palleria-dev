package com.yunfie.illustia.ui.app

import com.yunfie.illustia.BookmarkChromeState
import com.yunfie.illustia.HomeChromeState
import com.yunfie.illustia.IllustiaUiState
import com.yunfie.illustia.NovelChromeState
import com.yunfie.illustia.RankingChromeState
import com.yunfie.illustia.models.Illust
import com.yunfie.illustia.models.LoadState
import com.yunfie.illustia.models.NovelPreview
import com.yunfie.illustia.models.UserPreview
import com.yunfie.illustia.settings.AppSettings

internal data class IllustiaAppStateBundle(
    val state: IllustiaUiState,
    val settings: AppSettings,
    val loadState: LoadState,
    val homeItems: List<Illust>,
    val novelItems: List<NovelPreview>,
    val timelineItems: List<Illust>,
    val rankingItems: List<Illust>,
    val bookmarkItems: List<Illust>,
    val watchlistItems: List<Illust>,
    val followingUsers: List<UserPreview>,
    val homeChrome: HomeChromeState,
    val novelChrome: NovelChromeState,
    val rankingChrome: RankingChromeState,
    val bookmarkChrome: BookmarkChromeState,
)
