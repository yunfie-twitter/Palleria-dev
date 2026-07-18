package com.yunfie.illustia.wallpaper

import com.yunfie.illustia.settings.AppSettings
import com.yunfie.illustia.settings.db.SavedIllustEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LiveWallpaperSelectionTest : FunSpec({
    test("newest order advances and wraps") {
        val older = saved(path = "older", savedAt = 10)
        val newer = saved(path = "newer", savedAt = 20)
        val settings = AppSettings(liveWallpaperOrder = "newest")

        selectCandidate(listOf(older, newer), settings, "newer", true)?.localCoverPath shouldBe "older"
        selectCandidate(listOf(older, newer), settings, "older", true)?.localCoverPath shouldBe "newer"
    }

    test("a single candidate remains selectable") {
        val only = saved(path = "only", savedAt = 10)

        selectCandidate(listOf(only), AppSettings(), "only", true) shouldBe only
    }
})

private fun saved(path: String, savedAt: Long) = SavedIllustEntity().apply {
    illustId = savedAt
    title = path
    artistName = "artist"
    artistId = 1
    thumbUrl = ""
    localCoverPath = path
    localPagePathsJson = "[]"
    pageCount = 1
    this.savedAt = savedAt
    saveGroup = "artist"
    xRestrict = 0
}
