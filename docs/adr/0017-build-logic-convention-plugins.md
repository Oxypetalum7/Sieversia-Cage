# ADR-0017: ビルドの共通設定を build-logic の convention plugin にまとめる

- ステータス: 採択
- 日付: 2026-10-04
- 関連: ADR-0003（ツールチェーンと依存バージョンの固定）, ADR-0016

## 背景

feature を切り出すと（ADR-0016）、モジュールが 8 つになる。`compileSdk 37.1`・`minSdk 29`・Java 17・Compose・Hilt の設定が、各モジュールの `build.gradle.kts` に重複する。ライブラリや SDK の版を上げるとき（ADR-0003）に、直す場所が散らばる。

## 検討した選択肢

1. **各 `build.gradle.kts` にそのまま書く** — 仕組みは単純。重複が増え、設定がずれやすい
2. **`build-logic/`（included build）に convention plugin を作る** — 共通設定を1か所にまとめ、各モジュールはプラグインを当てるだけにする。最初に作る手間がかかる

## 決定

選択肢2。`build-logic/` に次の convention plugin を作る（名前は作るときに確定する）。

- JVM ライブラリ（`:core:sim`）
- Android ライブラリ（`:core:*`。SDK・Java の版）
- Android feature（Android ライブラリ＋Compose＋Hilt＋feature の共通依存）
- Android アプリ（`:app`）

ktlint と detekt の適用は、今のルートの `subprojects { }` から convention plugin へ移す。

## 理由

- 版を上げるときに直す場所が1か所になり、ADR-0003 の「追従は意識的なタスク」が軽くなる
- feature の追加が、プラグインを当てるだけで済む

## 影響

- `build-logic` から Version Catalog（`libs`）を参照する仕組みが必要になる
- 切り出しの作業は「build-logic を作る → 既存の 3 モジュールに当てる → feature を切り出す」の順で行い、各段階でビルドが通ることを確かめる
