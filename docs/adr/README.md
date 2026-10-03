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
