mod dto;

pub(crate) use dto::{IllustDetailResponse, IllustPageResponse, UserDetailResponse};

#[derive(Debug, uniffi::Record)]
pub struct ApiResponse {
    pub status: u16,
    pub body: String,
}

#[derive(Debug, uniffi::Record)]
pub struct LoginSession {
    pub access_token: String,
    pub refresh_token: String,
    pub user_id: Option<u64>,
}

#[derive(Debug, uniffi::Record)]
pub struct IllustPage {
    pub items: Vec<Illust>,
    pub next_url: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct Illust {
    pub id: i64,
    pub title: String,
    pub illust_type: String,
    pub caption: String,
    pub artist_id: i64,
    pub artist_name: String,
    pub artist_avatar_url: Option<String>,
    pub square_image_url: String,
    pub medium_image_url: String,
    pub image_url: String,
    pub original_image_url: Option<String>,
    pub medium_image_pages: Vec<String>,
    pub image_pages: Vec<String>,
    pub original_image_pages: Vec<String>,
    pub tags: Vec<String>,
    pub page_count: i32,
    pub is_bookmarked: bool,
    pub total_comments: Option<i32>,
    pub series: Option<IllustSeries>,
}

#[derive(Debug, uniffi::Record)]
pub struct IllustSeries {
    pub id: i64,
    pub title: Option<String>,
}

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

#[derive(Clone, Debug, uniffi::Record)]
pub struct UgoiraFrame {
    pub file: String,
    pub delay_millis: i32,
}

#[derive(Debug, uniffi::Record)]
pub struct UgoiraPlayback {
    pub frames: Vec<UgoiraFrame>,
}
