# Rust API client

Pixiv APIのHTTPトランスポートとレスポンスJSONの検証・モデル変換は`pixiv-api`クレートで実装し、UniFFI生成の型付きKotlinバインディングを介して利用します。レスポンス本文を文字列としてKotlinへ戻さず、Rust側で`serde` DTOへ変換してからアプリモデルへ橋渡しします。

初回のみAndroid向けRustツールを準備してください。

```powershell
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
```

通常のGradleビルドでは、`preBuild`がKotlinバインディング生成と3 ABIの`.so`生成を自動実行します。NDKはAndroid SDKのインストール済み最新版を`cargo-ndk`が検出します。

## 品質確認

成功JSONはRust側でストリームデコードされ、展開後16 MiBまでに制限されます。小説HTMLは8 MiB、HTTPエラー本文は64 KiBまで読み取り、例外へ含める詳細は4 KiBまでです。wire DTOとUniFFI公開モデルは`src/models/`以下でドメイン別に分離しています。

```powershell
cd rust/pixiv-api
cargo fmt --check
cargo clippy --all-targets --all-features -- -D warnings
cargo test --all-features
cargo bench --features bench --bench illust_decode
```
