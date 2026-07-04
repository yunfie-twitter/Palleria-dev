package com.yunfie.illustia.settings.store

import androidx.datastore.preferences.core.Preferences
import com.yunfie.illustia.settings.AppSettings
import com.yunfie.illustia.settings.db.AccountEntity
import com.yunfie.illustia.settings.db.FavoriteTagEntity
import com.yunfie.illustia.settings.db.IllustiaDatabase
import com.yunfie.illustia.settings.db.SearchHistoryEntity
import com.yunfie.illustia.settings.db.SettingsDao
import com.yunfie.illustia.settings.db.ViewHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal suspend fun readRoomSettingsData(dao: SettingsDao): RoomSettingsData = withContext(Dispatchers.IO) {
    RoomSettingsData(
        searchHistory = dao.getSearchHistory(),
        favoriteTags = dao.getFavoriteTags(),
        viewHistory = dao.getViewHistory(),
        accounts = dao.getAccounts(),
    )
}

internal suspend fun writeRoomSettingsData(
    database: IllustiaDatabase,
    dao: SettingsDao,
    settings: AppSettings,
) = withContext(Dispatchers.IO) {
    database.runInTransaction {
        dao.clearSearchHistory()
        dao.insertSearchHistory(
            settings.searchHistory.take(MAX_SEARCH_HISTORY).mapIndexed { index, query ->
                SearchHistoryEntity(query, index)
            },
        )

        dao.clearFavoriteTags()
        dao.insertFavoriteTags(
            settings.favoriteTags.mapIndexed { index, tag ->
                FavoriteTagEntity(tag, index)
            },
        )

        dao.clearViewHistory()
        dao.insertViewHistory(
            settings.viewHistory.take(MAX_VIEW_HISTORY).mapIndexed { index, illust ->
                ViewHistoryEntity(
                    illust.id,
                    illust.title,
                    illust.artistName,
                    illust.imageUrl,
                    illust.pageCount,
                    illust.type,
                    index,
                )
            },
        )

        dao.clearAccounts()
        dao.insertAccounts(
            settings.accounts.mapIndexed { index, account ->
                AccountEntity(
                    account.userId,
                    account.name,
                    account.account,
                    account.profileImageUrl,
                    index,
                )
            },
        )
    }
}

internal fun savedIllustStorageBytes(directory: File): Long {
    if (!directory.exists()) return 0L
    return directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
