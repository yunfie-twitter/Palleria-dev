mod endpoints;
mod oauth;
mod transport;
mod ugoira_download;

use std::collections::HashMap;
use std::net::SocketAddr;
use std::time::Duration;

use reqwest::blocking::Client;

use crate::config::{ECH_HOSTS, ECH_IPS};
use crate::error::{ApiError, invalid_request, network_error};
use crate::headers::PixivHeaders;
use crate::models::{
    ApiResponse, Illust, IllustPage, LoginSession, UgoiraFrame, UgoiraPlayback, UserProfile,
};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum NetworkMode {
    Standard,
    Compat,
    Ech,
}

impl NetworkMode {
    fn parse(value: &str) -> Result<Self, ApiError> {
        match value {
            "standard" => Ok(Self::Standard),
            "compat" => Ok(Self::Compat),
            "ech" => Ok(Self::Ech),
            _ => Err(invalid_request(format!("unknown network mode: {value}"))),
        }
    }
}

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
        let network_mode = NetworkMode::parse(&network_mode)?;
        let mut builder = Client::builder()
            .connect_timeout(Duration::from_secs(12))
            .timeout(Duration::from_secs(30))
            .pool_max_idle_per_host(6)
            .pool_idle_timeout(Duration::from_secs(300))
            .tcp_keepalive(Duration::from_secs(60));

        if network_mode == NetworkMode::Ech {
            for host in ECH_HOSTS {
                for ip in ECH_IPS {
                    let address: SocketAddr = format!("{ip}:443").parse().map_err(|error| {
                        invalid_request(format!("invalid compatible endpoint: {error}"))
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
        transport::execute(self, method, url, headers, body, content_type)
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
        endpoints::illust_page(&response.body)
    }

    pub fn execute_illust_detail(
        &self,
        method: String,
        url: String,
        headers: HashMap<String, String>,
        body: Vec<u8>,
        content_type: Option<String>,
    ) -> Result<Illust, ApiError> {
        let response = self.execute(method, url, headers, body, content_type)?;
        endpoints::illust_detail(&response.body)
    }

    pub fn execute_user_profile(
        &self,
        method: String,
        url: String,
        headers: HashMap<String, String>,
        body: Vec<u8>,
        content_type: Option<String>,
        fallback_user_id: i64,
    ) -> Result<UserProfile, ApiError> {
        let response = self.execute(method, url, headers, body, content_type)?;
        endpoints::user_profile(&response.body, fallback_user_id)
    }

    pub fn prepare_ugoira(
        &self,
        url: String,
        headers: HashMap<String, String>,
        cache_dir: String,
        frames: Vec<UgoiraFrame>,
    ) -> Result<UgoiraPlayback, ApiError> {
        ugoira_download::prepare(self, url, headers, cache_dir, frames)
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

    #[test]
    fn rejects_an_unknown_network_mode() {
        let result = PixivHttpClient::new(
            "unknown".into(),
            "test".into(),
            "Android 10".into(),
            "en-US".into(),
        );

        assert!(matches!(
            result,
            Err(ApiError::InvalidRequest { detail }) if detail == "unknown network mode: unknown"
        ));
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

    #[test]
    fn parses_illust_detail_before_crossing_the_ffi_boundary() {
        let response = client()
            .execute_illust_detail(
                "GET".into(),
                server(200, r#"{"illust":{"id":7}}"#),
                HashMap::new(),
                vec![],
                None,
            )
            .unwrap();
        assert_eq!(response.id, 7);
    }

    #[test]
    fn parses_user_profile_before_crossing_the_ffi_boundary() {
        let response = client()
            .execute_user_profile(
                "GET".into(),
                server(200, r#"{"user":{"name":"artist"},"profile":{}}"#),
                HashMap::new(),
                vec![],
                None,
                8,
            )
            .unwrap();
        assert_eq!(response.id, 8);
        assert_eq!(response.name, "artist");
    }
}
