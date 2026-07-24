use std::collections::HashMap;

use reqwest::blocking::{RequestBuilder, Response};
use reqwest::header::{CONTENT_TYPE, HeaderMap, HeaderName, HeaderValue};

use super::PixivHttpClient;
use crate::error::{ApiError, http_error, invalid_request, network_error};
use crate::models::ApiResponse;

pub(super) fn execute(
    client: &PixivHttpClient,
    method: String,
    url: String,
    headers: HashMap<String, String>,
    body: Vec<u8>,
    content_type: Option<String>,
) -> Result<ApiResponse, ApiError> {
    let method = reqwest::Method::from_bytes(method.as_bytes())
        .map_err(|error| invalid_request(format!("invalid HTTP method: {error}")))?;
    let headers = request_headers(client, headers, content_type.as_deref())?;
    let mut request = client.client.request(method, url).headers(headers);
    if !body.is_empty() {
        request = request.body(body);
    }
    send_text(request)
}

pub(super) fn request_headers(
    client: &PixivHttpClient,
    headers: HashMap<String, String>,
    content_type: Option<&str>,
) -> Result<HeaderMap, ApiError> {
    let mut header_map = HeaderMap::with_capacity(headers.len() + 9);
    for (name, value) in headers {
        let name = HeaderName::from_bytes(name.as_bytes())
            .map_err(|error| invalid_request(format!("invalid header name: {error}")))?;
        let value = HeaderValue::from_str(&value)
            .map_err(|error| invalid_request(format!("invalid header value: {error}")))?;
        header_map.append(name, value);
    }
    if let Some(value) = content_type {
        let value = HeaderValue::from_str(value)
            .map_err(|error| invalid_request(format!("invalid content type: {error}")))?;
        header_map.insert(CONTENT_TYPE, value);
    }
    client.headers.for_request(header_map)
}

pub(super) fn send_text(request: RequestBuilder) -> Result<ApiResponse, ApiError> {
    read_text(request.send().map_err(network_error)?)
}

fn read_text(response: Response) -> Result<ApiResponse, ApiError> {
    let status = response.status().as_u16();
    let body = response.text().map_err(network_error)?;
    if !(200..300).contains(&status) {
        return Err(http_error(status, &body));
    }
    Ok(ApiResponse { status, body })
}
