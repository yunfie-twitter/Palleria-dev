#[derive(Debug, uniffi::Record)]
pub struct NovelPage {
    pub items: Vec<NovelPreview>,
    pub next_url: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct NovelPreview {
    pub id: i64,
    pub title: String,
    pub caption: String,
    pub user_id: i64,
    pub user_name: String,
    pub user_account: String,
    pub cover_url: String,
    pub page_count: i32,
    pub text_length: i32,
    pub is_bookmarked: bool,
    pub total_bookmarks: i32,
    pub total_view: i32,
}

#[derive(Debug, uniffi::Record)]
pub struct WatchlistMangaPage {
    pub series: Vec<MangaSeries>,
    pub next_url: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct MangaSeries {
    pub id: i64,
    pub url: Option<String>,
    pub published_content_count: i32,
    pub title: String,
    pub user: Option<MangaSeriesUser>,
    pub last_published_content_datetime: Option<String>,
    pub latest_content_id: i64,
    pub thumbnail_url: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct MangaSeriesUser {
    pub id: i64,
    pub name: String,
    pub account: Option<String>,
    pub profile_image_url: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct NovelText {
    pub novel_id: i64,
    pub title: String,
    pub text: String,
    pub series_prev_id: Option<i64>,
    pub series_prev_title: Option<String>,
    pub series_next_id: Option<i64>,
    pub series_next_title: Option<String>,
}
