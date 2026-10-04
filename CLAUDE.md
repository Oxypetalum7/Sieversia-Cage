# Sieversia Cage

## 決定済みの方針（覆すときは新しい ADR を起こす）

- Android 専用に振り切る。DI は Hilt、KMP は採らない。ただし `:core:sim` は Android にも Remote Compose にも依存しない純 Kotlin（JVM）の壁を守る（iOS のためではなく、テスト容易性・差し替え余地・境界・Phase 2 のため）。ADR-0015
- 設計判断は `docs/adr/` に ADR として残す。一覧は `docs/adr/README.md`
- 開発発注書は `docs/brief.md`（ローカルにだけ置き、リポジトリには含めない）。スパイクの記録は `docs/spikes/README.md`
