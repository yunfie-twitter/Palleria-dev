mod client;
mod config;
mod error;
mod headers;
mod image_analysis;
mod models;
mod ugoira;

pub use client::PixivHttpClient;
pub use error::ApiError;
pub use image_analysis::{ImageAnalysis, analyze_rgba};
pub use models::{
    ApiResponse, Illust, IllustPage, IllustSeries, LoginSession, UgoiraFrame, UgoiraPlayback,
    UserProfile,
};

uniffi::setup_scaffolding!();
