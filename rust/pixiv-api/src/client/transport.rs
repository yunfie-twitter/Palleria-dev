use std::collections::HashMap;
use std::io::{self, BufReader, Read};

use reqwest::blocking::{RequestBuilder, Response};
use reqwest::header::{CONTENT_TYPE, HeaderMap, HeaderName, HeaderValue};
use serde::de::DeserializeOwned;

use super::PixivHttpClient;
use crate::error::{ApiError, http_error, invalid_request, invalid_response, network_error};
use crate::models::PixivRequest;

pub(super) const JSON_RESPONSE_LIMIT: u64 = 16 * 1024 * 1024;
pub(super) const HTML_RESPONSE_LIMIT: u64 = 8 * 1024 * 1024;
const ERROR_RESPONSE_LIMIT: u64 = 64 * 1024;
const ERROR_DETAIL_LIMIT: usize = 4 * 1024;
const NO_CONTENT_DRAIN_LIMIT: u64 = 64 * 1024;
const LIMIT_ERROR: &str = "decoded response exceeds configured limit";

pub(super) fn execute_json<T: DeserializeOwned>(
    client: &PixivHttpClient,
    request: PixivRequest,
    context: &str,
) -> Result<T, ApiError> {
    send_json(build_request(client, request)?, context)
}

pub(super) fn execute_text(
    client: &PixivHttpClient,
    request: PixivRequest,
    context: &str,
    limit: u64,
) -> Result<String, ApiError> {
    send_text(build_request(client, request)?, context, limit)
}

pub(super) fn execute_no_content(
    client: &PixivHttpClient,
    request: PixivRequest,
) -> Result<(), ApiError> {
    send_no_content(build_request(client, request)?)
}

fn build_request(
    client: &PixivHttpClient,
    request: PixivRequest,
) -> Result<RequestBuilder, ApiError> {
    let method = reqwest::Method::from_bytes(request.method.as_bytes())
        .map_err(|error| invalid_request(format!("invalid HTTP method: {error}")))?;
    let headers = request_headers(client, request.headers, request.content_type.as_deref())?;
    let mut builder = client.client.request(method, request.url).headers(headers);
    if !request.body.is_empty() {
        builder = builder.body(request.body);
    }
    Ok(builder)
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

pub(super) fn send_json<T: DeserializeOwned>(
    request: RequestBuilder,
    context: &str,
) -> Result<T, ApiError> {
    let response = ensure_success(request.send().map_err(network_error)?)?;
    reject_declared_size(&response, JSON_RESPONSE_LIMIT, context)?;
    let reader = BufReader::new(LimitedReader::new(response, JSON_RESPONSE_LIMIT));
    serde_json::from_reader(reader).map_err(|error| {
        if error.to_string().contains(LIMIT_ERROR) {
            response_too_large(context, JSON_RESPONSE_LIMIT)
        } else {
            invalid_response(format!("invalid {context} response: {error}"))
        }
    })
}

pub(super) fn send_text(
    request: RequestBuilder,
    context: &str,
    limit: u64,
) -> Result<String, ApiError> {
    let response = ensure_success(request.send().map_err(network_error)?)?;
    reject_declared_size(&response, limit, context)?;
    read_limited_text(response, limit).map_err(|error| map_read_error(error, context, limit))
}

pub(super) fn send_no_content(request: RequestBuilder) -> Result<(), ApiError> {
    let mut response = ensure_success(request.send().map_err(network_error)?)?;
    let _ = io::copy(
        &mut response.by_ref().take(NO_CONTENT_DRAIN_LIMIT),
        &mut io::sink(),
    );
    Ok(())
}

pub(super) fn ensure_success(response: Response) -> Result<Response, ApiError> {
    let status = response.status().as_u16();
    if (200..300).contains(&status) {
        return Ok(response);
    }
    let mut body = Vec::new();
    let _ = response.take(ERROR_RESPONSE_LIMIT).read_to_end(&mut body);
    Err(http_error(status, &error_preview(&body)))
}

fn reject_declared_size(response: &Response, limit: u64, context: &str) -> Result<(), ApiError> {
    if response
        .content_length()
        .is_some_and(|length| length > limit)
    {
        return Err(response_too_large(context, limit));
    }
    Ok(())
}

fn read_limited_text(response: Response, limit: u64) -> Result<String, io::Error> {
    let mut text = String::new();
    LimitedReader::new(response, limit).read_to_string(&mut text)?;
    Ok(text)
}

fn map_read_error(error: io::Error, context: &str, limit: u64) -> ApiError {
    if error.to_string().contains(LIMIT_ERROR) {
        response_too_large(context, limit)
    } else {
        invalid_response(format!("invalid {context} response body: {error}"))
    }
}

fn response_too_large(context: &str, limit: u64) -> ApiError {
    invalid_response(format!(
        "{context} response exceeds the {} MiB limit",
        limit / (1024 * 1024)
    ))
}

fn error_preview(body: &[u8]) -> String {
    let normalized = String::from_utf8_lossy(body)
        .split_whitespace()
        .collect::<Vec<_>>()
        .join(" ");
    if normalized.is_empty() {
        return "Pixiv API request failed".to_owned();
    }
    truncate_utf8(&normalized, ERROR_DETAIL_LIMIT)
}

fn truncate_utf8(value: &str, max_bytes: usize) -> String {
    if value.len() <= max_bytes {
        return value.to_owned();
    }
    let mut end = max_bytes;
    while !value.is_char_boundary(end) {
        end -= 1;
    }
    format!("{}…", &value[..end])
}

struct LimitedReader<R> {
    inner: R,
    remaining: u64,
    checked_end: bool,
}

impl<R> LimitedReader<R> {
    fn new(inner: R, limit: u64) -> Self {
        Self {
            inner,
            remaining: limit,
            checked_end: false,
        }
    }
}

impl<R: Read> Read for LimitedReader<R> {
    fn read(&mut self, buffer: &mut [u8]) -> io::Result<usize> {
        if buffer.is_empty() || self.checked_end {
            return Ok(0);
        }
        if self.remaining == 0 {
            let mut probe = [0_u8; 1];
            let read = self.inner.read(&mut probe)?;
            self.checked_end = true;
            return if read == 0 {
                Ok(0)
            } else {
                Err(io::Error::other(LIMIT_ERROR))
            };
        }
        let allowed = usize::try_from(self.remaining.min(buffer.len() as u64))
            .expect("limited read size must fit usize");
        let read = self.inner.read(&mut buffer[..allowed])?;
        self.remaining -= read as u64;
        Ok(read)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Cursor;
    use std::io::Write;
    use std::net::TcpListener;
    use std::thread;

    fn client() -> PixivHttpClient {
        PixivHttpClient::new(
            "standard".into(),
            "test".into(),
            "Android 10".into(),
            "en-US".into(),
        )
        .unwrap()
    }

    fn raw_server(response: Vec<u8>) -> String {
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let address = format!("http://{}", listener.local_addr().unwrap());
        thread::spawn(move || {
            let (mut stream, _) = listener.accept().unwrap();
            let mut request = [0_u8; 1024];
            let _ = stream.read(&mut request);
            stream.write_all(&response).unwrap();
        });
        address
    }

    fn response(status: &str, headers: &str, body: &[u8]) -> Vec<u8> {
        let mut response =
            format!("HTTP/1.1 {status}\r\nConnection: close\r\n{headers}\r\n").into_bytes();
        response.extend_from_slice(body);
        response
    }

    #[test]
    fn limited_reader_accepts_exact_limit() {
        let mut output = String::new();
        LimitedReader::new(Cursor::new(b"exact"), 5)
            .read_to_string(&mut output)
            .unwrap();
        assert_eq!(output, "exact");
    }

    #[test]
    fn limited_reader_rejects_one_byte_over_limit() {
        let mut output = Vec::new();
        let error = LimitedReader::new(Cursor::new(b"large"), 4)
            .read_to_end(&mut output)
            .unwrap_err();
        assert_eq!(error.to_string(), LIMIT_ERROR);
        assert_eq!(output, b"larg");
    }

    #[test]
    fn error_preview_is_bounded_and_utf8_safe() {
        let body = "界".repeat(ERROR_DETAIL_LIMIT);
        let preview = error_preview(body.as_bytes());
        assert!(preview.len() <= ERROR_DETAIL_LIMIT + "…".len());
        assert!(preview.ends_with('…'));
    }

    #[test]
    fn accepts_an_exact_size_body_without_content_length() {
        let url = raw_server(response("200 OK", "", b"four"));
        let text = send_text(client().client.get(url), "test", 4).unwrap();
        assert_eq!(text, "four");
    }

    #[test]
    fn rejects_an_oversized_body_without_content_length() {
        let url = raw_server(response("200 OK", "", b"large"));
        let error = send_text(client().client.get(url), "test", 4).unwrap_err();
        assert!(matches!(error, ApiError::InvalidResponse { .. }));
    }

    #[test]
    fn rejects_a_declared_size_above_the_limit_before_reading() {
        let url = raw_server(response("200 OK", "Content-Length: 100\r\n", b"x"));
        let error = send_text(client().client.get(url), "test", 4).unwrap_err();
        assert!(matches!(error, ApiError::InvalidResponse { .. }));
    }

    #[test]
    fn truncates_large_http_error_details() {
        let body = vec![b'x'; ERROR_RESPONSE_LIMIT as usize + 1024];
        let url = raw_server(response(
            "500 Internal Server Error",
            &format!("Content-Length: {}\r\n", body.len()),
            &body,
        ));
        let error = send_no_content(client().client.get(url)).unwrap_err();
        assert!(matches!(
            error,
            ApiError::Http {
                status: 500,
                detail
            } if detail.len() <= ERROR_DETAIL_LIMIT + "…".len()
        ));
    }

    #[test]
    fn accepts_an_empty_no_content_response() {
        let url = raw_server(response("204 No Content", "", b""));
        send_no_content(client().client.get(url)).unwrap();
    }
}
