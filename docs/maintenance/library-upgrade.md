# Remote Compose の版を上げるときのチェックリスト

Remote Compose は alpha なので、版上げは意識的なタスクとして扱う（ADR-0003）。上げるときはこのリストを上から順に進め、結果を ADR に残す。

## 1. 事前に調べる

- [ ] Google Maven で新しい版を確認する。`androidx.compose.remote:*` は全アーティファクトが同じ版で揃って配布される。`androidx.compose.remote.foundation:foundation` が要求する版も確認する
- [ ] リリースノートと、新しい版の AAR メタデータ（`minCompileSdk` / `minSdk` / `minAndroidGradlePluginVersion`）を確認する
- [ ] `CoreDocument.DOCUMENT_API_LEVEL` とヘッダーのバージョンが変わったかを sources jar で確認する
- [ ] Pixel 6a の `RemoteViews.DrawInstructions.getSupportedVersion()` と比べ、ウィジェットに載せるドキュメントの API レベルが端末を超えないことを確認する

## 2. 版を上げる

- [ ] ADR を起こす（何から何へ上げるか、理由、確認結果）
- [ ] `gradle/libs.versions.toml` の `remoteCompose` / `remoteComposeFoundation` を更新する

## 3. RestrictTo の依存箇所を直す（ADR-0010）

- [ ] 作成側: `:core:compiler`（`WriterPlantDocumentCompiler` など）
- [ ] 再生側: プレイヤーを包むコード（スパイク中は `:app` の `PlantDocumentView.kt`、feature 切り出し後は専用モジュール）
- [ ] 上記以外に `RestrictedApi` の抑止が増えていないことを確認する（`grep -rn 'RestrictedApi'`）

## 4. 回帰を確認する

- [ ] `./gradlew :core:sim:test :app:assembleDebug ktlintCheck detekt` が通る
- [ ] **古い .rc を新しいプレイヤーで読めるか**: `docs/spikes/samples/*.rc` を新しい版で読み込み、`toNestedString()` のダンプと描画を確認する。読めなくなったファイルと原因を ADR に記録する
- [ ] 実機（Pixel 6a）でアプリ内再生とウィジェット表示を確認する
- [ ] スパイク記録（`docs/spikes/README.md`）の既知の落とし穴が直っていないか確認する（例: プレイヤーが 0x0 になる、`obtain()` が説明文を捨てる）。直っていれば回避策を外す

## 5. アルバムを移行する（ADR-0011）

- [ ] 描き方や生成結果が変わる場合は、Compiler の版を上げる
- [ ] アプリ起動時のマイグレーションで、メタデータの版が古い日が今の Compiler で作り直されることを確認する
- [ ] 読込に失敗する日が残っていないことを確認する
- [ ] 新しい版で作った .rc を `docs/spikes/samples/`（または回帰用のサンプル置き場）に追加し、次回の版上げで「古い .rc」として使う
