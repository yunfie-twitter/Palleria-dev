use super::*;

impl UserDetailResponse {
    pub(crate) fn into_profile(self, fallback_user_id: i64) -> UserProfile {
        let profile = self.profile.unwrap_or_default();
        UserProfile {
            id: if self.user.id > 0 {
                self.user.id
            } else {
                fallback_user_id
            },
            name: self.user.name,
            account: self.user.account,
            profile_image_url: self.user.profile_image_urls.medium,
            background_image_url: profile.background_image_url,
            comment: profile.comment,
            is_followed: self.user.is_followed.unwrap_or(false),
        }
    }
}

impl AutocompleteResponse {
    pub(crate) fn into_list(self) -> StringList {
        StringList {
            items: self.tags.into_iter().filter_map(|tag| tag.name).collect(),
        }
    }
}

impl UserPreviewPageResponse {
    pub(crate) fn into_page(self) -> UserPreviewPage {
        UserPreviewPage {
            items: self
                .user_previews
                .into_iter()
                .filter_map(UserPreviewDto::into_preview)
                .collect(),
            next_url: self.next_url,
        }
    }
}

impl UserPreviewDto {
    fn into_preview(self) -> Option<UserPreview> {
        let user = self.user?;
        let id = (user.id > 0).then_some(user.id)?;
        Some(UserPreview {
            id,
            name: user.name,
            account: user.account,
            profile_image_url: user.profile_image_urls.medium,
            comment: user.comment.unwrap_or_default(),
            is_followed: user.is_followed.unwrap_or(false),
            preview_illusts: self
                .illusts
                .into_iter()
                .filter_map(IllustDto::into_illust)
                .collect(),
        })
    }
}

impl UserFollowDetailResponse {
    pub(crate) fn into_detail(self) -> UserFollowDetail {
        UserFollowDetail {
            is_followed: self.is_followed.or(self.is_follow).unwrap_or(false),
            restrict: self.restrict,
        }
    }
}

impl UserMeResponse {
    pub(crate) fn into_profile(self) -> Option<CurrentUserProfile> {
        let profile = self.profile?;
        Some(CurrentUserProfile {
            user_id: profile.user_id?,
            pixiv_id: profile.pixiv_id,
            name: profile.name,
            profile_image_url: profile.profile_image_urls.and_then(|urls| urls.medium),
            is_premium: profile.is_premium,
            x_restrict: profile.x_restrict,
        })
    }
}

impl AccountEditResponse {
    pub(crate) fn into_result(self) -> AccountEditResult {
        let is_succeeded = self
            .body
            .as_ref()
            .and_then(|body| body.is_succeed)
            .unwrap_or(!self.error);
        let validation_errors = self
            .body
            .map(|body| {
                body.validation_errors
                    .into_iter()
                    .map(|(key, value)| {
                        let message = value.as_str().map(str::to_owned).unwrap_or_else(|| {
                            if value.is_null() {
                                String::new()
                            } else {
                                value.to_string()
                            }
                        });
                        (key, message)
                    })
                    .collect()
            })
            .unwrap_or_default();
        AccountEditResult {
            is_succeeded,
            message: self.message,
            validation_errors,
        }
    }
}
