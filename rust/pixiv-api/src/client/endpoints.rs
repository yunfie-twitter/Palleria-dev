use serde::de::DeserializeOwned;

use crate::error::{ApiError, invalid_response};
use crate::models::{
    Illust, IllustDetailResponse, IllustPage, IllustPageResponse, UserDetailResponse, UserProfile,
};

pub(super) fn illust_page(body: &str) -> Result<IllustPage, ApiError> {
    decode::<IllustPageResponse>(body, "illust page").map(IllustPageResponse::into_page)
}

pub(super) fn illust_detail(body: &str) -> Result<Illust, ApiError> {
    decode::<IllustDetailResponse>(body, "illust detail")?
        .into_illust()
        .ok_or_else(|| invalid_response("illust detail response does not contain a valid illust"))
}

pub(super) fn user_profile(body: &str, fallback_user_id: i64) -> Result<UserProfile, ApiError> {
    decode::<UserDetailResponse>(body, "user detail")
        .map(|response| response.into_profile(fallback_user_id))
}

fn decode<T: DeserializeOwned>(body: &str, context: &str) -> Result<T, ApiError> {
    serde_json::from_str(body)
        .map_err(|error| invalid_response(format!("invalid {context} response: {error}")))
}
