use std::collections::HashMap;
use std::net::SocketAddr;
use std::time::Duration;

use chrono::Utc;
use reqwest::blocking::Client;
use reqwest::header::{
    ACCEPT, ACCEPT_LANGUAGE, AUTHORIZATION, CONTENT_TYPE, HeaderMap, HeaderName, HeaderValue,
    REFERER, USER_AGENT,
};

const APP_VERSION: &str = "5.0.166";
const CLIENT_ID: &str = "MOBrBDS8blbauoSck0ZfDbtuzpyT";
const CLIENT_SECRET: &str = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj";
const CLIENT_HASH_SECRET: &str = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c";
const REDIRECT_URI: &str = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback";

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ApiError {
    #[error("invalid request: {detail}")]
    InvalidRequest { detail: String },
    #[error("network request failed: {detail}")]
    Network { detail: String },
    #[error("HTTP {status}: {detail}")]
    Http { status: u16, detail: String },
}

#[derive(Debug, uniffi::Record)]
pub struct ApiResponse {
    pub status: u16,
    pub body: Vec<u8>,
}

#[derive(Debug, uniffi::Record)]
pub struct LoginSession {
    pub access_token: String,
    pub refresh_token: String,
    pub user_id: Option<u64>,
}

#[derive(uniffi::Object)]
pub struct PixivHttpClient {
    client: Client,
    user_agent: String,
    app_os_version: String,
    accept_language: String,
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
            .pool_idle_timeout(Duration::from_secs(300));

        if network_mode != "standard" {
            builder = builder
                .danger_accept_invalid_certs(true)
                .danger_accept_invalid_hostnames(true);
        }
        if network_mode == "ech" {
            for host in [
                "app-api.pixiv.net",
                "oauth.secure.pixiv.net",
                "accounts.pixiv.net",
            ] {
                for ip in ["104.18.10.118", "104.18.11.118"] {
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

        let client = builder.build().map_err(network_error)?;
        Ok(Self {
            client,
            user_agent,
            app_os_version,
            accept_language,
        })
    }

    pub fn login_with_refresh_token(
        &self,
        refresh_token: String,
    ) -> Result<LoginSession, ApiError> {
        let token = refresh_token.trim();
        if token.is_empty() {
            return Err(ApiError::InvalidRequest {
                detail: "refresh token is empty".into(),
            });
        }
        self.oauth_login(
            vec![
                ("grant_type", "refresh_token".into()),
                ("include_policy", "true".into()),
                ("refresh_token", token.into()),
            ],
            Some(token),
        )
    }

    pub fn login_with_authorization_code(
        &self,
        code: String,
        code_verifier: String,
    ) -> Result<LoginSession, ApiError> {
        self.oauth_login(
            vec![
                ("grant_type", "authorization_code".into()),
                ("include_policy", "true".into()),
                ("code", code),
                ("code_verifier", code_verifier),
                ("redirect_uri", REDIRECT_URI.into()),
            ],
            None,
        )
    }

    pub fn create_web_login_url(
        &self,
        create_provisional_account: bool,
        code_challenge: String,
    ) -> String {
        let path = if create_provisional_account {
            "provisional-accounts/create"
        } else {
            "login"
        };
        let encoded: String =
            reqwest::Url::parse(&format!("https://app-api.pixiv.net/web/v1/{path}"))
                .expect("static Pixiv login URL must be valid")
                .query_pairs_mut()
                .append_pair("code_challenge", &code_challenge)
                .append_pair("code_challenge_method", "S256")
                .append_pair("client", "pixiv-android")
                .finish()
                .to_string();
        encoded
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
        let mut header_map = HeaderMap::new();
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

        self.apply_pixiv_headers(&mut header_map)?;

        let mut request = self.client.request(method, &url).headers(header_map);
        if !body.is_empty() {
            request = request.body(body);
        }
        let response = request.send().map_err(network_error)?;
        let status = response.status().as_u16();
        let bytes = response.bytes().map_err(network_error)?.to_vec();
        if !(200..300).contains(&status) {
            let detail = pixiv_error_message(&bytes);
            return Err(ApiError::Http { status, detail });
        }
        Ok(ApiResponse {
            status,
            body: bytes,
        })
    }
}

impl PixivHttpClient {
    fn apply_pixiv_headers(&self, headers: &mut HeaderMap) -> Result<(), ApiError> {
        let client_time = Utc::now().format("%Y-%m-%dT%H:%M:%S+00:00").to_string();
        let client_hash = format!(
            "{:x}",
            md5::compute(format!("{client_time}{CLIENT_HASH_SECRET}"))
        );
        insert_header(headers, USER_AGENT, &self.user_agent)?;
        insert_named_header(headers, "app-os", "Android")?;
        insert_named_header(headers, "app-os-version", &self.app_os_version)?;
        insert_named_header(headers, "app-version", APP_VERSION)?;
        insert_header(headers, ACCEPT_LANGUAGE, &self.accept_language)?;
        insert_named_header(headers, "x-client-time", &client_time)?;
        insert_named_header(headers, "x-client-hash", &client_hash)?;
        if headers.contains_key(AUTHORIZATION) {
            headers.insert(ACCEPT, HeaderValue::from_static("application/json"));
            headers.insert(REFERER, HeaderValue::from_static("https://www.pixiv.net/"));
        }
        Ok(())
    }

    fn oauth_login(
        &self,
        mut fields: Vec<(&str, String)>,
        fallback_refresh_token: Option<&str>,
    ) -> Result<LoginSession, ApiError> {
        fields.push(("client_id", CLIENT_ID.into()));
        fields.push(("client_secret", CLIENT_SECRET.into()));
        let mut headers = HeaderMap::new();
        self.apply_pixiv_headers(&mut headers)?;
        let response = self
            .client
            .post("https://oauth.secure.pixiv.net/auth/token")
            .headers(headers)
            .form(&fields)
            .send()
            .map_err(network_error)?;
        let status = response.status().as_u16();
        let bytes = response.bytes().map_err(network_error)?.to_vec();
        if !(200..300).contains(&status) {
            return Err(ApiError::Http {
                status,
                detail: pixiv_error_message(&bytes),
            });
        }
        let root: serde_json::Value =
            serde_json::from_slice(&bytes).map_err(|error| ApiError::InvalidRequest {
                detail: format!("invalid OAuth response: {error}"),
            })?;
        let response = root.get("response").unwrap_or(&root);
        let access_token = response
            .get("access_token")
            .and_then(|value| value.as_str())
            .ok_or_else(|| ApiError::InvalidRequest {
                detail: "OAuth response has no access token".into(),
            })?
            .into();
        let refresh_token = response
            .get("refresh_token")
            .and_then(|value| value.as_str())
            .or(fallback_refresh_token)
            .ok_or_else(|| ApiError::InvalidRequest {
                detail: "OAuth response has no refresh token".into(),
            })?
            .into();
        let user_id = response
            .get("user")
            .and_then(|user| user.get("id"))
            .and_then(|id| id.as_u64().or_else(|| id.as_str()?.parse().ok()));
        Ok(LoginSession {
            access_token,
            refresh_token,
            user_id,
        })
    }
}

fn insert_header(headers: &mut HeaderMap, name: HeaderName, value: &str) -> Result<(), ApiError> {
    let value = HeaderValue::from_str(value).map_err(|error| ApiError::InvalidRequest {
        detail: format!("invalid Pixiv header: {error}"),
    })?;
    headers.insert(name, value);
    Ok(())
}

fn insert_named_header(
    headers: &mut HeaderMap,
    name: &'static str,
    value: &str,
) -> Result<(), ApiError> {
    insert_header(headers, HeaderName::from_static(name), value)
}

fn network_error(error: reqwest::Error) -> ApiError {
    ApiError::Network {
        detail: error.to_string(),
    }
}

fn pixiv_error_message(body: &[u8]) -> String {
    let text = String::from_utf8_lossy(body).trim().to_owned();
    if text.is_empty() {
        "Pixiv API request failed".to_owned()
    } else {
        text
    }
}

uniffi::setup_scaffolding!();

#[cfg(test)]
mod tests {
    use super::*;
    use std::thread;

    fn server(status: u16, body: &'static str) -> String {
        let server = tiny_http::Server::http("127.0.0.1:0").unwrap();
        let address = format!("http://{}", server.server_addr());
        thread::spawn(move || {
            let request = server.recv().unwrap();
            request
                .respond(tiny_http::Response::from_string(body).with_status_code(status))
                .unwrap();
        });
        address
    }

    #[test]
    fn returns_success_body() {
        let client = PixivHttpClient::new(
            "standard".into(),
            "test".into(),
            "Android 10".into(),
            "en-US".into(),
        )
        .unwrap();
        let response = client
            .execute(
                "GET".into(),
                server(200, "{\"ok\":true}"),
                HashMap::new(),
                vec![],
                None,
            )
            .unwrap();
        assert_eq!(response.status, 200);
        assert_eq!(response.body, br#"{"ok":true}"#);
    }

    #[test]
    fn exposes_http_status_and_body() {
        let client = PixivHttpClient::new(
            "standard".into(),
            "test".into(),
            "Android 10".into(),
            "en-US".into(),
        )
        .unwrap();
        let error = client
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
}
