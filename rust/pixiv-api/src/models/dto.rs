use serde::Deserialize;

use super::{Illust, IllustPage, IllustSeries, UserProfile};

#[derive(Debug, Deserialize)]
pub(crate) struct IllustPageResponse {
    #[serde(default)]
    illusts: Vec<IllustDto>,
    next_url: Option<String>,
}

#[derive(Debug, Deserialize)]
pub(crate) struct IllustDetailResponse {
    illust: Option<IllustDto>,
}

#[derive(Debug, Deserialize)]
pub(crate) struct UserDetailResponse {
    #[serde(default)]
    user: UserDto,
    profile: Option<ProfileDto>,
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
    account: String,
    #[serde(default)]
    profile_image_urls: ImageUrls,
    #[serde(default)]
    is_followed: bool,
}

#[derive(Debug, Default, Deserialize)]
struct ProfileDto {
    background_image_url: Option<String>,
    #[serde(default)]
    comment: String,
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

impl IllustDetailResponse {
    pub(crate) fn into_illust(self) -> Option<Illust> {
        self.illust.and_then(IllustDto::into_illust)
    }
}

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
            is_followed: self.user.is_followed,
        }
    }
}

impl IllustDto {
    fn into_illust(self) -> Option<Illust> {
        let id = self.id?;
        let medium_pages = page_urls(&self.meta_pages, |urls| {
            urls.medium.as_ref().or(urls.large.as_ref())
        });
        let image_pages = page_urls(&self.meta_pages, |urls| {
            urls.large.as_ref().or(urls.medium.as_ref())
        });
        let original_pages = page_urls(&self.meta_pages, |urls| {
            urls.original.as_ref().or(urls.large.as_ref())
        });
        let original = self
            .meta_single_page
            .original
            .clone()
            .or_else(|| original_pages.first().cloned());
        let medium = self
            .image_urls
            .medium
            .clone()
            .or_else(|| self.image_urls.square_medium.clone())
            .or_else(|| self.image_urls.large.clone())
            .or_else(|| original.clone())
            .unwrap_or_default();
        let image_url = self
            .image_urls
            .large
            .clone()
            .or_else(|| self.image_urls.medium.clone())
            .or_else(|| original.clone())
            .unwrap_or_default();

        Some(Illust {
            id,
            title: self.title,
            illust_type: self.illust_type,
            caption: self.caption,
            artist_id: self.user.id,
            artist_name: self.user.name,
            artist_avatar_url: self.user.profile_image_urls.medium,
            square_image_url: self.image_urls.square_medium.unwrap_or_default(),
            medium_image_url: medium,
            image_url,
            original_image_url: original,
            medium_image_pages: fallback_pages(medium_pages, &image_pages, &original_pages),
            image_pages: fallback_pages(image_pages, &original_pages, &[]),
            original_image_pages: original_pages,
            tags: self.tags.into_iter().filter_map(|tag| tag.name).collect(),
            page_count: self.page_count,
            is_bookmarked: self.is_bookmarked,
            total_comments: self.total_comments,
            series: self.series.map(|series| IllustSeries {
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

    #[test]
    fn parses_an_illust_detail() {
        let response: IllustDetailResponse =
            serde_json::from_str(r#"{"illust":{"id":42,"title":"detail"}}"#).unwrap();
        let illust = response.into_illust().unwrap();
        assert_eq!(illust.id, 42);
        assert_eq!(illust.title, "detail");
    }

    #[test]
    fn parses_a_user_detail_and_uses_the_requested_id_as_fallback() {
        let response: UserDetailResponse = serde_json::from_str(
            r#"{
                "user": {
                    "name": "artist",
                    "account": "artist-account",
                    "profile_image_urls": {"medium": "avatar"},
                    "is_followed": true
                },
                "profile": {
                    "background_image_url": "background",
                    "comment": "hello"
                }
            }"#,
        )
        .unwrap();
        let profile = response.into_profile(99);
        assert_eq!(profile.id, 99);
        assert_eq!(profile.account, "artist-account");
        assert_eq!(profile.profile_image_url.as_deref(), Some("avatar"));
        assert!(profile.is_followed);
    }
}
