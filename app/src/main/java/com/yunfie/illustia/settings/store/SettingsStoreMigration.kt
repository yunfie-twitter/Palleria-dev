package com.yunfie.illustia.settings.store

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.yunfie.illustia.settings.db.IllustiaDatabase
import com.yunfie.illustia.settings.db.SettingsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

internal suspend fun migrateSettingsIfNeeded(
    dataStore: DataStore<Preferences>,
    encryptedPreferences: SharedPreferences?,
    legacyPreferences: SharedPreferences,
    database: IllustiaDatabase,
    dao: SettingsDao,
) = withContext(Dispatchers.IO) {
    val current = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .first()
    if ((current[SETTINGS_VERSION] ?: 0) >= CURRENT_SETTINGS_VERSION) return@withContext

    val source = when {
        encryptedPreferences != null && encryptedPreferences.all.isNotEmpty() -> encryptedPreferences
        else -> legacyPreferences
    }
    val sensitivePreferences = encryptedPreferences ?: legacyPreferences
    val migratedFromSharedPreferences = readFromSharedPreferences(source)
    val migratedFromDataStore = readCollectionsFromDataStore(current, sensitivePreferences)
    val hadExistingData = source.all.isNotEmpty() || current.asMap().isNotEmpty()
    val migrated = migratedFromSharedPreferences.copy(
        searchHistory = migratedFromDataStore.searchHistory.ifEmpty { migratedFromSharedPreferences.searchHistory },
        favoriteTags = migratedFromDataStore.favoriteTags.ifEmpty { migratedFromSharedPreferences.favoriteTags },
        viewHistory = migratedFromDataStore.viewHistory.ifEmpty { migratedFromSharedPreferences.viewHistory },
        accounts = migratedFromDataStore.accounts.ifEmpty { migratedFromSharedPreferences.accounts },
        onboardingSetupCompleted = hadExistingData,
    )
    dataStore.edit { preferences ->
        writeToDataStore(preferences, migrated)
    }
    writeSensitiveSettings(sensitivePreferences, migrated)
    writeRoomSettingsData(database, dao, migrated)
    legacyPreferences.edit()
        .remove(KEY_REFRESH_TOKEN)
        .remove(KEY_ACCOUNTS)
        .remove(KEY_ACTIVE_ACCOUNT_INDEX)
        .apply()
}
