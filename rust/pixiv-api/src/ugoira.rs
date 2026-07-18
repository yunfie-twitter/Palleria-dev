use std::collections::HashSet;
use std::fs::{self, File};
use std::io::{self, Read};
use std::path::{Component, Path, PathBuf};
use std::sync::{Mutex, OnceLock};

use zip::ZipArchive;

use crate::error::ApiError;
use crate::models::{UgoiraFrame, UgoiraPlayback};

const MAX_ENTRIES: usize = 10_000;
const MAX_ENTRY_BYTES: u64 = 32 * 1024 * 1024;
const MAX_TOTAL_BYTES: u64 = 512 * 1024 * 1024;
const COMPLETE_MARKER: &str = ".complete";

static PREPARE_LOCK: OnceLock<Mutex<()>> = OnceLock::new();

pub(crate) fn cached(cache_dir: &Path, frames: &[UgoiraFrame]) -> Option<UgoiraPlayback> {
    validate_frame_names(frames).ok()?;
    cache_is_complete(cache_dir, frames).then(|| playback(cache_dir, frames.to_vec()))
}

pub(crate) fn prepare(
    zip_path: &Path,
    cache_dir: &Path,
    frames: Vec<UgoiraFrame>,
) -> Result<UgoiraPlayback, ApiError> {
    let _guard = PREPARE_LOCK
        .get_or_init(|| Mutex::new(()))
        .lock()
        .map_err(|_| invalid("ugoira preparation lock is poisoned"))?;

    validate_frame_names(&frames)?;
    if cache_is_complete(cache_dir, &frames) {
        return Ok(playback(cache_dir, frames));
    }

    let parent = cache_dir
        .parent()
        .ok_or_else(|| invalid("ugoira cache directory has no parent"))?;
    fs::create_dir_all(parent).map_err(io_error)?;
    let staging = staging_path(cache_dir);
    remove_if_exists(&staging)?;
    fs::create_dir_all(&staging).map_err(io_error)?;

    let result = extract_required(zip_path, &staging, &frames).and_then(|()| {
        File::create(staging.join(COMPLETE_MARKER)).map_err(io_error)?;
        remove_if_exists(cache_dir)?;
        fs::rename(&staging, cache_dir).map_err(io_error)
    });
    if result.is_err() {
        let _ = fs::remove_dir_all(&staging);
    }
    result?;
    Ok(playback(cache_dir, frames))
}

fn extract_required(
    zip_path: &Path,
    staging: &Path,
    frames: &[UgoiraFrame],
) -> Result<(), ApiError> {
    let file = File::open(zip_path).map_err(io_error)?;
    let mut archive = ZipArchive::new(file).map_err(zip_error)?;
    if archive.len() > MAX_ENTRIES {
        return Err(invalid("ugoira archive contains too many entries"));
    }

    let required: HashSet<&str> = frames.iter().map(|frame| frame.file.as_str()).collect();
    let mut extracted = HashSet::with_capacity(required.len());
    let mut total_bytes = 0_u64;

    for index in 0..archive.len() {
        let mut entry = archive.by_index(index).map_err(zip_error)?;
        let name = entry.name().replace('\\', "/");
        validate_relative_path(&name)?;
        if entry
            .unix_mode()
            .is_some_and(|mode| mode & 0o170000 == 0o120000)
        {
            return Err(invalid("ugoira archive contains a symbolic link"));
        }
        if entry.is_dir() || !required.contains(name.as_str()) {
            continue;
        }
        if entry.size() > MAX_ENTRY_BYTES {
            return Err(invalid("ugoira frame exceeds the size limit"));
        }
        total_bytes = total_bytes
            .checked_add(entry.size())
            .ok_or_else(|| invalid("ugoira archive size overflow"))?;
        if total_bytes > MAX_TOTAL_BYTES {
            return Err(invalid("ugoira archive exceeds the total size limit"));
        }

        let target = staging.join(&name);
        if let Some(parent) = target.parent() {
            fs::create_dir_all(parent).map_err(io_error)?;
        }
        let mut output = File::create(target).map_err(io_error)?;
        let expected_size = entry.size();
        let copied = io::copy(&mut entry.by_ref().take(MAX_ENTRY_BYTES + 1), &mut output)
            .map_err(io_error)?;
        if copied > MAX_ENTRY_BYTES {
            return Err(invalid("ugoira frame exceeds the size limit"));
        }
        if copied != expected_size {
            return Err(invalid("ugoira frame size does not match its ZIP metadata"));
        }
        extracted.insert(name);
    }

    if required.iter().any(|name| !extracted.contains(*name)) {
        return Err(invalid("ugoira archive is missing required frames"));
    }
    Ok(())
}

fn validate_frame_names(frames: &[UgoiraFrame]) -> Result<(), ApiError> {
    if frames.is_empty() {
        return Err(invalid("ugoira metadata contains no frames"));
    }
    if frames.len() > MAX_ENTRIES {
        return Err(invalid("ugoira metadata contains too many frames"));
    }
    for frame in frames {
        validate_relative_path(&frame.file)?;
    }
    Ok(())
}

fn validate_relative_path(name: &str) -> Result<(), ApiError> {
    let path = Path::new(name);
    if name.is_empty()
        || name.contains('\\')
        || path.is_absolute()
        || path.components().any(|component| {
            matches!(
                component,
                Component::ParentDir | Component::RootDir | Component::Prefix(_)
            )
        })
    {
        return Err(invalid("ugoira archive contains an unsafe path"));
    }
    Ok(())
}

fn cache_is_complete(cache_dir: &Path, frames: &[UgoiraFrame]) -> bool {
    cache_dir.join(COMPLETE_MARKER).is_file()
        && frames
            .iter()
            .all(|frame| cache_dir.join(&frame.file).is_file())
}

fn playback(cache_dir: &Path, frames: Vec<UgoiraFrame>) -> UgoiraPlayback {
    UgoiraPlayback {
        frames: frames
            .into_iter()
            .map(|frame| UgoiraFrame {
                file: cache_dir.join(frame.file).to_string_lossy().into_owned(),
                delay_millis: frame.delay_millis.max(20),
            })
            .collect(),
    }
}

fn staging_path(cache_dir: &Path) -> PathBuf {
    let file_name = cache_dir
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or("ugoira");
    cache_dir.with_file_name(format!("{file_name}.staging"))
}

fn remove_if_exists(path: &Path) -> Result<(), ApiError> {
    if path.is_dir() {
        fs::remove_dir_all(path).map_err(io_error)?;
    } else if path.exists() {
        fs::remove_file(path).map_err(io_error)?;
    }
    Ok(())
}

fn invalid(detail: &str) -> ApiError {
    ApiError::InvalidRequest {
        detail: detail.into(),
    }
}

fn io_error(error: io::Error) -> ApiError {
    ApiError::Network {
        detail: error.to_string(),
    }
}

fn zip_error(error: zip::result::ZipError) -> ApiError {
    ApiError::InvalidRequest {
        detail: format!("invalid ugoira ZIP: {error}"),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use zip::write::SimpleFileOptions;

    fn temp_dir(name: &str) -> PathBuf {
        let nonce = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        let dir =
            std::env::temp_dir().join(format!("palleria-{name}-{}-{nonce}", std::process::id(),));
        let _ = fs::remove_dir_all(&dir);
        fs::create_dir_all(&dir).unwrap();
        dir
    }

    fn zip_with(path: &Path, entries: &[(&str, &[u8])]) {
        let file = File::create(path).unwrap();
        let mut writer = zip::ZipWriter::new(file);
        for (name, contents) in entries {
            writer
                .start_file(*name, SimpleFileOptions::default())
                .unwrap();
            writer.write_all(contents).unwrap();
        }
        writer.finish().unwrap();
    }

    #[test]
    fn extracts_only_required_frames_and_marks_cache_complete() {
        let root = temp_dir("extract");
        let zip_path = root.join("frames.zip");
        zip_with(&zip_path, &[("000.jpg", b"a"), ("unused.jpg", b"b")]);
        let cache = root.join("cache");
        let result = prepare(
            &zip_path,
            &cache,
            vec![UgoiraFrame {
                file: "000.jpg".into(),
                delay_millis: 10,
            }],
        )
        .unwrap();
        assert_eq!(result.frames[0].delay_millis, 20);
        assert!(cache.join("000.jpg").is_file());
        assert!(!cache.join("unused.jpg").exists());
        assert!(cache.join(COMPLETE_MARKER).is_file());
        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn rejects_unsafe_frame_paths() {
        let root = temp_dir("unsafe");
        let result = prepare(
            &root.join("missing.zip"),
            &root.join("cache"),
            vec![UgoiraFrame {
                file: "../escape.jpg".into(),
                delay_millis: 20,
            }],
        );
        assert!(matches!(result, Err(ApiError::InvalidRequest { .. })));
        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn rejects_archives_missing_required_frames() {
        let root = temp_dir("missing");
        let zip_path = root.join("frames.zip");
        zip_with(&zip_path, &[("other.jpg", b"x")]);
        let result = prepare(
            &zip_path,
            &root.join("cache"),
            vec![UgoiraFrame {
                file: "000.jpg".into(),
                delay_millis: 20,
            }],
        );
        assert!(matches!(result, Err(ApiError::InvalidRequest { .. })));
        fs::remove_dir_all(root).unwrap();
    }
}
