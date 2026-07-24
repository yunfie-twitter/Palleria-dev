use criterion::{Criterion, black_box, criterion_group, criterion_main};
use palleria_pixiv_api::benchmark_decode_illust_page;

fn large_illust_page() -> Vec<u8> {
    let item = include_str!("fixtures/illust.json").trim();
    format!(
        r#"{{"illusts":[{}],"next_url":"https://example.invalid/next"}}"#,
        std::iter::repeat_n(item, 250).collect::<Vec<_>>().join(",")
    )
    .into_bytes()
}

fn decode_illust_page(criterion: &mut Criterion) {
    let fixture = large_illust_page();
    criterion.bench_function("decode 250 illusts", |bencher| {
        bencher.iter(|| {
            let count = benchmark_decode_illust_page(black_box(&fixture)).unwrap();
            black_box(count)
        });
    });
}

criterion_group!(benches, decode_illust_page);
criterion_main!(benches);
