# ADR-0003: ツールチェーンと依存バージョンの固定

- ステータス: 採択
- 日付: 2026-10-03
- 関連: ADR-0002, 発注書「技術前提」（alphaリスクと対処）

## 背景

Remote Compose 系はすべて alpha で API 変更が速い。発注書の方針どおり Gradle バージョンカタログ（`gradle/libs.versions.toml`）で全バージョンを固定し、追従は意識的なタスクとして扱う。プロジェクト初期化前に固定値を決める。

2026-10-03 時点で Google Maven / Maven Central を確認した結果、発注書の記載から以下が進んでいた。

| 項目 | 発注書の記載 | 実際の最新 |
| --- | --- | --- |
| `androidx.compose.remote:*` | 1.0.0-alpha01 | **1.0.0-alpha20**（全アーティファクト同一版で揃って配布） |
| `androidx.compose.remote.foundation:foundation` | 1.0.0-alpha02 / compileSdk 37.1 要求 | **1.0.0-alpha03**。AAR メタデータ上は `minCompileSdk=35` / `minSdk=29` / AGP 8.6.0 以上 |

なお Remote Compose の各 AAR は `minSdk=29`。開発用の実機は Pixel 6a（Android 16 = API 36）。

## 検討した選択肢

### Remote Compose の版

1. **発注書の版（alpha01 / foundation alpha02）に合わせる** — 記載との整合は取れるが、10か月分の修正・API整理を捨てることになる
2. **最新（alpha20 / foundation alpha03）で固定する** — 公開情報・サンプルとの乖離が出る可能性はあるが、foundation alpha03 が alpha20 を要求するため組み合わせとして一貫する

### minSdk

1. **minSdk 36（Android 16）** — ウィジェットの対象と一致し、分岐が不要。ただしショーケースも Android 16 以上でしか動かない
2. **minSdk 29（ライブラリ下限）** — ショーケース／アルバムは古い端末でも動き、発注書の「端末を選ばないデモ導線」と一致。ウィジェットは API 36 以上でのみ有効化する（リソース修飾子 `-v36` やランタイム判定）

## 決定

| 項目 | 版 |
| --- | --- |
| JDK | Android Studio 同梱 JBR 25（toolchain 指定は 21 で検討） |
| Gradle | 9.8.0 |
| AGP | 9.4.1（安定版の最新） |
| Kotlin | 2.4.20（安定版の最新） |
| compileSdk | 37.1（インストール済み） |
| targetSdk | 36 |
| minSdk | **29**（選択肢2） |
| `androidx.compose.remote:*` | 1.0.0-alpha20 |
| `androidx.compose.remote.foundation:foundation` | 1.0.0-alpha03 |
| Compose BOM | 2026.09.00（Compose UI 1.12.1。Remote Compose 側要求 1.11.0 を満たす） |
| WorkManager | 2.12.0 |

## 理由

- alpha20 + foundation alpha03 は依存宣言上の正しい組み合わせ。alpha01 に戻す利点が「発注書の記載と一致する」以外にない
- minSdk 29 なら、ウィジェット非対応端末でもショーケースが主役として成立する。初日スパイクの退路と相性が良い
- AGP・Kotlin は alpha/Beta を避けて安定版にし、不安定要因を Remote Compose だけに絞る

## 影響

- 発注書の「主要アーティファクト」表は版が古くなる。README 作成時に実際の版で書く
- minSdk 29 を選ぶと、ウィジェット関連のコードに API レベル判定が入る
- Remote Compose の版上げは、別タスクとして ADR を起こして行う
