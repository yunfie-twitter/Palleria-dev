package com.yunfie.illustia

import com.yunfie.illustia.models.Illust
import com.yunfie.illustia.models.UserPreview
import com.yunfie.illustia.models.UserProfile
import com.yunfie.illustia.models.pixiv.Comment

internal data class DetailSnapshot(
    val illust: Illust,
    val user: UserProfile?,
    val firstComment: Comment?,
    val relatedIllusts: List<Illust>,
)

internal data class SearchSnapshot(
    val searchDraft: String,
    val activeSearchWord: String,
    val searchItems: List<Illust>,
    val searchNextUrl: String?,
    val userSearchItems: List<UserPreview>,
    val userSearchNextUrl: String?,
)

internal data class UserPageSnapshot(
    val selectedUser: UserProfile?,
    val selectedUserIllusts: List<Illust>,
    val selectedUserNextUrl: String?,
    val selectedUserBookmarks: List<Illust>,
    val selectedUserBookmarksNextUrl: String?,
    val showUserPage: Boolean,
    val userPageFromSheet: Boolean,
    val userPageDismissed: Boolean,
)
