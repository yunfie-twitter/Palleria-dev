use super::IllustSeries;

#[derive(Debug, uniffi::Record)]
pub struct IllustSeriesPage {
    pub detail: Option<IllustSeriesDetail>,
    pub first_illust: Option<SeriesIllust>,
    pub illusts: Vec<SeriesIllust>,
    pub next_url: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct IllustSeriesDetail {
    pub height: i32,
    pub series_work_count: i32,
    pub id: i64,
    pub create_date: String,
    pub title: String,
    pub width: i32,
    pub cover_image_url: Option<String>,
    pub watchlist_added: bool,
    pub caption: String,
    pub user: Option<SeriesUser>,
}

#[derive(Debug, uniffi::Record)]
pub struct SeriesUser {
    pub id: i64,
    pub account: String,
    pub name: String,
    pub profile_image_url: Option<String>,
    pub is_followed: bool,
}

#[derive(Debug, uniffi::Record)]
pub struct SeriesIllust {
    pub id: i64,
    pub title: String,
    pub illust_type: String,
    pub square_image_url: String,
    pub medium_image_url: String,
    pub large_image_url: String,
    pub caption: String,
    pub restrict: i32,
    pub user: SeriesIllustUser,
    pub tags: Vec<String>,
    pub tools: Vec<String>,
    pub create_date: String,
    pub page_count: i32,
    pub width: i32,
    pub height: i32,
    pub sanity_level: i32,
    pub x_restrict: i32,
    pub has_meta_single_page: bool,
    pub original_image_url: Option<String>,
    pub meta_pages: Vec<SeriesImagePage>,
    pub total_view: i32,
    pub total_bookmarks: i32,
    pub is_bookmarked: bool,
    pub visible: bool,
    pub is_muted: bool,
    pub illust_ai_type: i32,
    pub series: Option<IllustSeries>,
    pub illust_book_style: Option<i32>,
    pub total_comments: Option<i32>,
}

#[derive(Debug, uniffi::Record)]
pub struct SeriesIllustUser {
    pub id: i64,
    pub name: String,
    pub account: String,
    pub profile_image_url: String,
    pub comment: Option<String>,
    pub is_followed: Option<bool>,
}

#[derive(Debug, uniffi::Record)]
pub struct SeriesImagePage {
    pub square_image_url: String,
    pub medium_image_url: String,
    pub large_image_url: String,
    pub original_image_url: String,
}
