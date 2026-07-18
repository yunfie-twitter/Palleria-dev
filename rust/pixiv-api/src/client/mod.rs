mod oauth;

use std::collections::HashMap;
use std::fs::{self, File};
use std::io::Write;
use std::net::SocketAddr;
use std::path::Path;
use std::sync::{Mutex, OnceLock};
use std::time::Duration;

use reqwest::blocking::Client;
use reqwest::header::{CONTENT_TYPE, HeaderMap, HeaderName, HeaderValue};

use crate::config::{ECH_HOSTS, ECH_IPS};
use crate::error::{ApiError, http_error, network_error};
use crate::headers::PixivHeaders;
use crate::models::{
    ApiResponse, IllustPage, IllustPageResponse, LoginSession, UgoiraFrame, UgoiraPlayback,
};
use crate::ugoira;

static UGOIRA_DOWNLOAD_LOCK: OnceLock<Mutex<()>> = OnceLock::new();

#[derive(uniffi::Object)]
pub struct PixivHttpClient {
    pub(super) client: Client,
    pub(super) headers: PixivHeaders,
}

#[uniffi::export]
impl PixivHttpClient {
    #[uniffi::constructor]
    pub fn new(
        network_mode: String,
        user_agent: String,
        app_os_version: String,
        accept_language: String,
    ) -> Result<Self, ApiError> {
        let mut builder = Client::builder()
            .connect_timeout(Duration::from_secs(12))
            .timeout(Duration::from_secs(30))
            .pool_max_idle_per_host(6)
            .pool_idle_timeout(Duration::from_secs(300))
            .tcp_keepalive(Duration::from_secs(60));

        if network_mode != "standard" {
            builder = builder
                .danger_accept_invalid_certs(true)
                .danger_accept_invalid_hostnames(true);
        }
        if network_mode == "ech" {
            for host in ECH_HOSTS {
                for ip in ECH_IPS {
                    let address: SocketAddr =
                        format!("{ip}:443")
                            .parse()
                            .map_err(|error| ApiError::InvalidRequest {
                                detail: format!("invalid compatible endpoint: {error}"),
                            })?;
                    builder = builder.resolve(host, address);
                }
            }
        }

        Ok(Self {
            client: builder.build().map_err(network_error)?,
            headers: PixivHeaders::new(&user_agent, &app_os_version, &accept_language)?,
        })
    }

    pub fn login_with_refresh_token(
        &self,
        refresh_token: String,
    ) -> Result<LoginSession, ApiError> {
        oauth::login_with_refresh_token(self, refresh_token)
    }

    pub fn login_with_authorization_code(
        &self,
        code: String,
        code_verifier: String,
    ) -> Result<LoginSession, ApiError> {
        oauth::login_with_authorization_code(self, code, code_verifier)
    }

    pub fn create_web_login_url(
        &self,
        create_provisional_account: bool,
        code_challenge: String,
    ) -> String {
        oauth::create_web_login_url(create_provisional_account, code_challenge)
    }

    pub fn execute(
        &self,
        method: String,
        url: String,
        headers: HashMap<String, String>,
        body: Vec<u8>,
        content_type: Option<String>,
    ) -> Result<ApiResponse, ApiError> {
        let method = reqwest::Method::from_bytes(method.as_bytes()).map_err(|error| {
            ApiError::InvalidRequest {
                detail: format!("invalid HTTP method: {error}"),
            }
        })?;
        let mut header_map = HeaderMap::with_capacity(headers.len() + 9);
        for (name, value) in headers {
            let name = HeaderName::from_bytes(name.as_bytes()).map_err(|error| {
                ApiError::InvalidRequest {
                    detail: format!("invalid header name: {error}"),
                }
            })?;
            let value =
                HeaderValue::from_str(&value).map_err(|error| ApiError::InvalidRequest {
                    detail: format!("invalid header value: {error}"),
                })?;
            header_map.append(name, value);
        }
        if let Some(value) = content_type {
            let value =
                HeaderValue::from_str(&value).map_err(|error| ApiError::InvalidRequest {
                    detail: format!("invalid content type: {error}"),
                })?;
            header_map.insert(CONTENT_TYPE, value);
        }

        let mut request = self
            .client
            .request(method, &url)
            .headers(self.headers.for_request(header_map)?);
        if !body.is_empty() {
            request = request.body(body);
        }
        let response = request.send().map_err(network_error)?;
        let status = response.status().as_u16();
        let body = response.text().map_err(network_error)?;
        if !(200..300).contains(&status) {
            return Err(http_error(status, &body));
        }
        Ok(ApiResponse { status, body })
    }

    pub fn execute_illust_page(
        &self,
        method: String,
        url: String,
        headers: HashMap<String, String>,
        body: Vec<u8>,
        content_type: Option<String>,
    ) -> Result<IllustPage, ApiError> {
        let response = self.execute(method, url, headers, body, content_type)?;
        serde_json::from_str::<IllustPageResponse>(&response.body)
            .map(IllustPageResponse::into_page)
            .map_err(|error| ApiError::InvalidRequest {
                detail: format!("invalid illust page response: {error}"),
            })
    }

    pub fn prepare_ugoira(
        &self,
        url: String,
        headers: HashMap<String, String>,
        cache_dir: String,
        frames: Vec<UgoiraFrame>,
    ) -> Result<UgoiraPlayback, ApiError> {
        let _guard = UGOIRA_DOWNLOAD_LOCK
            .get_or_init(|| Mutex::new(()))
            .lock()
            .map_err(|_| ApiError::InvalidRequest {
                detail: "ugoira download lock is poisoned".into(),
            })?;
        let cache_dir = Path::new(&cache_dir);
        if let Some(playback) = ugoira::cached(cache_dir, &frames) {
            return Ok(playback);
        }

        let mut header_map = HeaderMap::with_capacity(headers.len() + 9);
        for (name, value) in headers {
            let name = HeaderName::from_bytes(name.as_bytes()).map_err(|error| {
                ApiError::InvalidRequest {
                    detail: format!("invalid header name: {error}"),
                }
            })?;
            let value =
                HeaderValue::from_str(&value).map_err(|error| ApiError::InvalidRequest {
                    detail: format!("invalid header value: {error}"),
                })?;
            header_map.append(name, value);
        }

        let mut response = self
            .client
            .get(&url)
            .headers(self.headers.for_request(header_map)?)
            .send()
            .map_err(network_error)?;
        let status = response.status().as_u16();
        if !(200..300).contains(&status) {
            let body = response.text().map_err(network_error)?;
            return Err(http_error(status, &body));
        }

        let parent = cache_dir.parent().ok_or_else(|| ApiError::InvalidRequest {
            detail: "ugoira cache directory has no parent".into(),
        })?;
        fs::create_dir_all(parent).map_err(file_error)?;
        let zip_path = cache_dir.with_extension("zip.download");
        let result = (|| {
            let mut output = File::create(&zip_path).map_err(file_error)?;
            response.copy_to(&mut output).map_err(network_error)?;
            output.flush().map_err(file_error)?;
            ugoira::prepare(&zip_path, cache_dir, frames)
        })();
        let _ = fs::remove_file(zip_path);
        result
    }
}

fn file_error(error: std::io::Error) -> ApiError {
    ApiError::Network {
        detail: error.to_string(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
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

    fn server(status: u16, body: &'static str) -> String {
        let server = tiny_http::Server::http("127.0.0.1:0").unwrap();
        let address = format!("http://{}", server.server_addr());
        thread::spawn(move || {
            server
                .recv()
                .unwrap()
                .respond(tiny_http::Response::from_string(body).with_status_code(status))
                .unwrap();
        });
        address
    }

    #[test]
    fn returns_success_body() {
        let response = client()
            .execute(
                "GET".into(),
                server(200, "{\"ok\":true}"),
                HashMap::new(),
                vec![],
                None,
            )
            .unwrap();
        assert_eq!(response.status, 200);
        assert_eq!(response.body, r#"{"ok":true}"#);
    }

    #[test]
    fn exposes_http_status_and_body() {
        let error = client()
            .execute(
                "GET".into(),
                server(403, "denied"),
                HashMap::new(),
                vec![],
                None,
            )
            .unwrap_err();
        assert!(matches!(error, ApiError::Http { status: 403, detail } if detail == "denied"));
    }

    #[test]
    fn parses_illust_page_before_crossing_the_ffi_boundary() {
        let response = client()
            .execute_illust_page(
                "GET".into(),
                server(200, r#"{"illusts":[{"id":1}],"next_url":null}"#),
                HashMap::new(),
                vec![],
                None,
            )
            .unwrap();
        assert_eq!(response.items.len(), 1);
        assert_eq!(response.items[0].id, 1);
    }
}
