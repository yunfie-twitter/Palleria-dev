const TRANSPARENT_ALPHA: u8 = 16;
const DOMINANT_BUCKETS: usize = 16 * 16 * 16;

#[derive(Debug, uniffi::Record)]
pub struct ImageAnalysis {
    pub average_luminance: f64,
    pub dominant_argb: i32,
    pub sample_count: u32,
}

#[uniffi::export]
pub fn analyze_rgba(pixels: Vec<u8>) -> ImageAnalysis {
    let mut luminance_sum = 0.0;
    let mut sample_count = 0_u32;
    let mut buckets = vec![ColorBucket::default(); DOMINANT_BUCKETS];

    for pixel in pixels.chunks_exact(4) {
        let [red, green, blue, alpha] = [pixel[0], pixel[1], pixel[2], pixel[3]];
        if alpha < TRANSPARENT_ALPHA {
            continue;
        }

        luminance_sum += relative_luminance(red, green, blue);
        sample_count += 1;

        let index =
            ((red as usize >> 4) << 8) | ((green as usize >> 4) << 4) | (blue as usize >> 4);
        let bucket = &mut buckets[index];
        bucket.count += 1;
        bucket.red += red as u64;
        bucket.green += green as u64;
        bucket.blue += blue as u64;
    }

    let dominant_argb = buckets
        .into_iter()
        .max_by_key(|bucket| bucket.count)
        .filter(|bucket| bucket.count > 0)
        .map(|bucket| {
            let count = bucket.count as u64;
            argb(
                (bucket.red / count) as u8,
                (bucket.green / count) as u8,
                (bucket.blue / count) as u8,
            )
        })
        .unwrap_or(argb(0, 0, 0));

    ImageAnalysis {
        average_luminance: if sample_count == 0 {
            0.0
        } else {
            luminance_sum / f64::from(sample_count)
        },
        dominant_argb,
        sample_count,
    }
}

#[derive(Clone, Default)]
struct ColorBucket {
    count: u32,
    red: u64,
    green: u64,
    blue: u64,
}

fn relative_luminance(red: u8, green: u8, blue: u8) -> f64 {
    0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
}

fn linearize(channel: u8) -> f64 {
    let normalized = f64::from(channel) / 255.0;
    if normalized <= 0.03928 {
        normalized / 12.92
    } else {
        ((normalized + 0.055) / 1.055).powf(2.4)
    }
}

fn argb(red: u8, green: u8, blue: u8) -> i32 {
    u32::from_be_bytes([255, red, green, blue]) as i32
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn computes_known_black_and_white_luminance() {
        let black = analyze_rgba(vec![0, 0, 0, 255]);
        let white = analyze_rgba(vec![255, 255, 255, 255]);
        assert_eq!(black.average_luminance, 0.0);
        assert!((white.average_luminance - 1.0).abs() < f64::EPSILON);
    }

    #[test]
    fn ignores_transparent_pixels_and_finds_the_dominant_bucket() {
        let result = analyze_rgba(vec![
            255, 0, 0, 255, 250, 5, 5, 255, 0, 0, 255, 255, 255, 255, 255, 0,
        ]);
        assert_eq!(result.sample_count, 3);
        assert_eq!(result.dominant_argb, argb(252, 2, 2));
    }

    #[test]
    fn handles_empty_and_incomplete_buffers() {
        let result = analyze_rgba(vec![255, 0, 0]);
        assert_eq!(result.sample_count, 0);
        assert_eq!(result.dominant_argb, argb(0, 0, 0));
    }
}
