use std::collections::HashMap;
use std::fs;
use std::io::{Read, Write};
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex, OnceLock, Weak};

use super::{PixivHttpClient, transport};
use crate::error::{ApiError, invalid_request, io_error, network_error};
use crate::models::{UgoiraFrame, UgoiraPlayback};
use crate::temp_path::TempPath;
use crate::ugoira;

const MAX_ARCHIVE_BYTES: u64 = 512 * 1024 * 1024;
static CACHE_LOCKS: OnceLock<Mutex<HashMap<PathBuf, Weak<Mutex<()>>>>> = OnceLock::new();

pub(super) fn prepare(
    client: &PixivHttpClient,
    url: String,
    headers: HashMap<String, String>,
    cache_dir: String,
    frames: Vec<UgoiraFrame>,
) -> Result<UgoiraPlayback, ApiError> {
    let cache_dir = PathBuf::from(cache_dir);
    let cache_lock = cache_lock(&cache_dir)?;
    let _guard = cache_lock
        .lock()
        .map_err(|_| invalid_request("ugoira cache lock is poisoned"))?;

    if let Some(playback) = ugoira::cached(&cache_dir, &frames) {
        return Ok(playback);
    }

    let headers = transport::request_headers(client, headers, None)?;
    let response = client
        .client
        .get(&url)
        .headers(headers)
        .send()
        .map_err(network_error)?;
    let mut response = transport::ensure_success(response)?;
    if response
        .content_length()
        .is_some_and(|length| length > MAX_ARCHIVE_BYTES)
    {
        return Err(archive_too_large());
    }

    let parent = cache_dir
        .parent()
        .ok_or_else(|| invalid_request("ugoira cache directory has no parent"))?;
    fs::create_dir_all(parent).map_err(|error| io_error("create ugoira cache parent", error))?;
    let zip_path = TempPath::sibling(&cache_dir, "zip.download");
    download_and_extract(&mut response, &zip_path, &cache_dir, frames)
}

fn download_and_extract(
    response: &mut impl Read,
    zip_path: &TempPath,
    cache_dir: &Path,
    frames: Vec<UgoiraFrame>,
) -> Result<UgoiraPlayback, ApiError> {
    let mut output = zip_path
        .create_file()
        .map_err(|error| io_error("create ugoira download", error))?;
    copy_with_limit(response, &mut output, MAX_ARCHIVE_BYTES)?;
    output
        .flush()
        .map_err(|error| io_error("flush ugoira download", error))?;
    drop(output);
    ugoira::prepare(zip_path.path(), cache_dir, frames)
}

fn cache_lock(cache_dir: &Path) -> Result<Arc<Mutex<()>>, ApiError> {
    let locks = CACHE_LOCKS.get_or_init(|| Mutex::new(HashMap::new()));
    let mut locks = locks
        .lock()
        .map_err(|_| invalid_request("ugoira lock registry is poisoned"))?;
    locks.retain(|_, lock| lock.strong_count() > 0);
    if let Some(lock) = locks.get(cache_dir).and_then(Weak::upgrade) {
        return Ok(lock);
    }
    let lock = Arc::new(Mutex::new(()));
    locks.insert(cache_dir.to_path_buf(), Arc::downgrade(&lock));
    Ok(lock)
}

fn copy_with_limit(
    reader: &mut impl Read,
    writer: &mut impl Write,
    limit: u64,
) -> Result<u64, ApiError> {
    let copied = std::io::copy(&mut reader.take(limit + 1), writer)
        .map_err(|error| io_error("write ugoira download", error))?;
    if copied > limit {
        return Err(archive_too_large());
    }
    Ok(copied)
}

fn archive_too_large() -> ApiError {
    invalid_request("ugoira archive exceeds the download limit")
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Cursor;

    #[test]
    fn copies_a_response_within_the_archive_limit() {
        let mut input = Cursor::new(b"ugoira".to_vec());
        let mut output = Vec::new();

        let copied = copy_with_limit(&mut input, &mut output, 6).unwrap();

        assert_eq!(copied, 6);
        assert_eq!(output, b"ugoira");
    }

    #[test]
    fn rejects_a_response_that_exceeds_the_archive_limit() {
        let mut input = Cursor::new(b"oversized".to_vec());
        let mut output = Vec::new();

        let error = copy_with_limit(&mut input, &mut output, 4).unwrap_err();

        assert!(matches!(
            error,
            ApiError::InvalidRequest { detail }
                if detail == "ugoira archive exceeds the download limit"
        ));
        assert_eq!(output.len(), 5);
    }
}
