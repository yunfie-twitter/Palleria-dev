use super::*;

impl NovelPageResponse {
    pub(crate) fn into_page(self) -> NovelPage {
        NovelPage {
            items: self
                .novels
                .into_iter()
                .filter_map(NovelDto::into_preview)
                .collect(),
            next_url: self.next_url,
        }
    }
}

impl NovelDto {
    fn into_preview(self) -> Option<NovelPreview> {
        Some(NovelPreview {
            id: self.id?,
            title: self.title,
            caption: self.caption,
            user_id: self.user.id,
            user_name: self.user.name,
            user_account: self.user.account,
            cover_url: self.image_urls.medium.unwrap_or_default(),
            page_count: self.page_count,
            text_length: self.text_length,
            is_bookmarked: self.is_bookmarked,
            total_bookmarks: self.total_bookmarks,
            total_view: self.total_view,
        })
    }
}

impl WatchlistMangaResponse {
    pub(crate) fn into_page(self) -> WatchlistMangaPage {
        WatchlistMangaPage {
            series: self
                .series
                .into_iter()
                .filter_map(MangaSeriesDto::into_series)
                .collect(),
            next_url: self.next_url,
        }
    }
}

impl MangaSeriesDto {
    fn into_series(self) -> Option<MangaSeries> {
        Some(MangaSeries {
            id: self.id?,
            url: self.url,
            published_content_count: self.published_content_count,
            title: self.title,
            user: self.user.map(|user| MangaSeriesUser {
                id: user.id,
                name: user.name,
                account: user.account,
                profile_image_url: user.profile_image_urls.and_then(|urls| urls.medium),
            }),
            last_published_content_datetime: self.last_published_content_datetime,
            latest_content_id: self.latest_content_id,
            thumbnail_url: self.thumbnail_url,
        })
    }
}
