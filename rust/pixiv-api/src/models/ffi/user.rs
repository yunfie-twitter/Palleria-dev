use super::Illust;

#[derive(Debug, uniffi::Record)]
pub struct UserProfile {
    pub id: i64,
    pub name: String,
    pub account: String,
    pub profile_image_url: Option<String>,
    pub background_image_url: Option<String>,
    pub comment: String,
    pub is_followed: bool,
}

#[derive(Debug, uniffi::Record)]
pub struct UserPreviewPage {
    pub items: Vec<UserPreview>,
    pub next_url: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct UserPreview {
    pub id: i64,
    pub name: String,
    pub account: String,
    pub profile_image_url: Option<String>,
    pub comment: String,
    pub is_followed: bool,
    pub preview_illusts: Vec<Illust>,
}

#[derive(Debug, uniffi::Record)]
pub struct UserFollowDetail {
    pub is_followed: bool,
    pub restrict: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct CurrentUserProfile {
    pub user_id: i64,
    pub pixiv_id: String,
    pub name: String,
    pub profile_image_url: Option<String>,
    pub is_premium: bool,
    pub x_restrict: i32,
}

#[derive(Debug, uniffi::Record)]
pub struct AccountEditResult {
    pub is_succeeded: bool,
    pub message: String,
    pub validation_errors: std::collections::HashMap<String, String>,
}
