use serde::Deserialize;

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

#[derive(Clone, Debug, uniffi::Record)]
pub struct UgoiraFrame {
    pub file: String,
    pub delay_millis: i32,
}

#[derive(Debug, uniffi::Record)]
pub struct UgoiraPlayback {
    pub frames: Vec<UgoiraFrame>,
}

#[derive(Debug, Deserialize)]
pub(crate) struct IllustPageResponse {
    #[serde(default)]
    illusts: Vec<IllustDto>,
    next_url: Option<String>,
}

#[derive(Debug, Default, Deserialize)]
struct ImageUrls {
    square_medium: Option<String>,
    medium: Option<String>,
    large: Option<String>,
    original: Option<String>,
}

#[derive(Debug, Default, Deserialize)]
struct UserDto {
    #[serde(default)]
    id: i64,
    #[serde(default)]
    name: String,
    #[serde(default)]
    profile_image_urls: ImageUrls,
}

#[derive(Debug, Default, Deserialize)]
struct MetaPage {
    #[serde(default)]
    image_urls: ImageUrls,
}

#[derive(Debug, Default, Deserialize)]
struct TagDto {
    name: Option<String>,
}

#[derive(Debug, Deserialize)]
struct SeriesDto {
    #[serde(default)]
    id: i64,
    title: Option<String>,
}

#[derive(Debug, Deserialize)]
struct IllustDto {
    id: Option<i64>,
    #[serde(default)]
    title: String,
    #[serde(rename = "type", default = "default_illust_type")]
    illust_type: String,
    #[serde(default)]
    caption: String,
    #[serde(default)]
    user: UserDto,
    #[serde(default)]
    image_urls: ImageUrls,
    #[serde(default)]
    meta_single_page: ImageUrls,
    #[serde(default)]
    meta_pages: Vec<MetaPage>,
    #[serde(default)]
    tags: Vec<TagDto>,
    #[serde(default = "default_page_count")]
    page_count: i32,
    #[serde(default)]
    is_bookmarked: bool,
    total_comments: Option<i32>,
    series: Option<SeriesDto>,
}

fn default_illust_type() -> String {
    "illust".into()
}

fn default_page_count() -> i32 {
    1
}

impl IllustPageResponse {
    pub(crate) fn into_page(self) -> IllustPage {
        IllustPage {
            items: self
                .illusts
                .into_iter()
                .filter_map(IllustDto::into_illust)
                .collect(),
            next_url: self.next_url,
        }
    }
}

impl IllustDto {
    fn into_illust(self) -> Option<Illust> {
        let id = self.id?;
        let value = self;
        let medium_pages = page_urls(&value.meta_pages, |urls| {
            urls.medium.as_ref().or(urls.large.as_ref())
        });
        let image_pages = page_urls(&value.meta_pages, |urls| {
            urls.large.as_ref().or(urls.medium.as_ref())
        });
        let original_pages = page_urls(&value.meta_pages, |urls| {
            urls.original.as_ref().or(urls.large.as_ref())
        });
        let original = value
            .meta_single_page
            .original
            .clone()
            .or_else(|| original_pages.first().cloned());
        let medium = value
            .image_urls
            .medium
            .clone()
            .or_else(|| value.image_urls.square_medium.clone())
            .or_else(|| value.image_urls.large.clone())
            .or_else(|| original.clone())
            .unwrap_or_default();
        let image_url = value
            .image_urls
            .large
            .clone()
            .or_else(|| value.image_urls.medium.clone())
            .or_else(|| original.clone())
            .unwrap_or_default();

        Some(Illust {
            id,
            title: value.title,
            illust_type: value.illust_type,
            caption: value.caption,
            artist_id: value.user.id,
            artist_name: value.user.name,
            artist_avatar_url: value.user.profile_image_urls.medium,
            square_image_url: value.image_urls.square_medium.unwrap_or_default(),
            medium_image_url: medium,
            image_url,
            original_image_url: original,
            medium_image_pages: fallback_pages(medium_pages, &image_pages, &original_pages),
            image_pages: fallback_pages(image_pages, &original_pages, &[]),
            original_image_pages: original_pages,
            tags: value.tags.into_iter().filter_map(|tag| tag.name).collect(),
            page_count: value.page_count,
            is_bookmarked: value.is_bookmarked,
            total_comments: value.total_comments,
            series: value.series.map(|series| IllustSeries {
                id: series.id,
                title: series.title,
            }),
        })
    }
}

fn page_urls<'a>(
    pages: &'a [MetaPage],
    select: impl Fn(&'a ImageUrls) -> Option<&'a String>,
) -> Vec<String> {
    pages
        .iter()
        .filter_map(|page| select(&page.image_urls).cloned())
        .collect()
}

fn fallback_pages(primary: Vec<String>, secondary: &[String], tertiary: &[String]) -> Vec<String> {
    if !primary.is_empty() {
        primary
    } else if !secondary.is_empty() {
        secondary.to_vec()
    } else {
        tertiary.to_vec()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_illust_page_and_applies_kotlin_compatible_fallbacks() {
        let response: IllustPageResponse = serde_json::from_str(
            r#"{
                "illusts": [{
                    "id": 42,
                    "title": "sample",
                    "type": "manga",
                    "user": {
                        "id": 7,
                        "name": "artist",
                        "profile_image_urls": {"medium": "avatar"}
                    },
                    "image_urls": {"square_medium": "square", "large": "large"},
                    "meta_pages": [{
                        "image_urls": {"medium": "page-medium", "large": "page-large", "original": "page-original"}
                    }],
                    "tags": [{"name": "tag"}],
                    "page_count": 1,
                    "is_bookmarked": true
                }],
                "next_url": "https://example.test/next"
            }"#,
        )
        .unwrap();

        let page = response.into_page();
        assert_eq!(page.next_url.as_deref(), Some("https://example.test/next"));
        assert_eq!(page.items.len(), 1);
        let illust = &page.items[0];
        assert_eq!(illust.id, 42);
        assert_eq!(illust.medium_image_url, "square");
        assert_eq!(illust.original_image_url.as_deref(), Some("page-original"));
        assert_eq!(illust.image_pages, ["page-large"]);
        assert_eq!(illust.tags, ["tag"]);
        assert!(illust.is_bookmarked);
    }

    #[test]
    fn skips_an_illust_without_an_id_like_the_kotlin_mapper() {
        let response =
            serde_json::from_str::<IllustPageResponse>(r#"{"illusts":[{"title":"missing id"}]}"#)
                .unwrap();
        assert!(response.into_page().items.is_empty());
    }
}
