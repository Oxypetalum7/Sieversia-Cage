# Architecture Decision Records

Sieversia Cage の設計判断の記録。提案と採択の経緯を残し、後から「なぜこうなっているか」を辿れるようにする。

## 運用ルール

- 1判断1ファイル。ファイル名は `NNNN-kebab-case-title.md`（連番4桁）
- テンプレートは [`template.md`](template.md)
- ステータス: `提案` → `採択` / `却下`。覆すときは元ADRを書き換えず、新ADRを起こして旧ADRを `廃止（ADR-NNNN により）` にする
- 採択済みADRの本文は原則編集しない（誤字・リンク修正は可）

## 一覧

| # | タイトル | ステータス | 日付 |
| --- | --- | --- | --- |
| [0001](0001-record-architecture-decisions.md) | 設計判断をADRで記録する | 採択 | 2026-10-03 |
| [0002](0002-spike-in-main-repo-with-modules.md) | スパイクを本リポジトリ内でモジュール分割して行う | 採択 | 2026-10-03 |
| [0003](0003-toolchain-and-dependency-versions.md) | ツールチェーンと依存バージョンの固定 | 採択 | 2026-10-03 |
| [0004](0004-module-layout-feature-core.md) | モジュール命名を feature/core 規約に揃え、段階的に分割する | 採択 | 2026-10-03 |
| [0005](0005-project-standards-adoption-timing.md) | 開発標準の導入タイミング | 採択 | 2026-10-03 |
| [0006](0006-application-id.md) | applicationId とパッケージ名 | 廃止（ADR-0007 により） | 2026-10-03 |
| [0007](0007-package-name-without-underscore.md) | パッケージ名からアンダースコアを外す | 採択 | 2026-10-03 |
| [0008](0008-detekt-on-jdk21-daemon.md) | detekt は安定版 1.23.8 を使い、Gradle デーモンを JDK 21 に固定する | 採択 | 2026-10-03 |
| [0009](0009-low-level-writer-as-creation-api.md) | ドキュメント作成には低レベル API（RemoteComposeWriter）を使う | 採択 | 2026-10-03 |
| [0010](0010-restricted-api-isolation.md) | RestrictTo の Remote Compose API を使い、依存箇所を閉じ込める | 採択 | 2026-10-03 |
| [0011](0011-album-regenerate-from-sim-state.md) | アルバムの正は成長データとし、.rc はライブラリ更新時に作り直す | 採択 | 2026-10-04 |
| [0012](0012-widget-profile-v6.md) | ウィジェットのドキュメントは WIDGETS_V6 で書き、シェーダーを使わない | 提案 | 2026-10-04 |
