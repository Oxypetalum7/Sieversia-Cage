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
- [ ] `minCompileSdk` などが上がっていたら、SDK・Java の版を `build-logic/convention/src/main/kotlin/ProjectConventions.kt` で上げる（各モジュールには書かない。ADR-0017）

## 3. RestrictTo の依存箇所を直す（ADR-0010）

- [ ] 作成側: `:core:compiler`（`WriterPlantDocumentCompiler` など）
- [ ] 再生側: `:core:player`（`PlantDocumentView.kt`。ADR-0016）
- [ ] 上記以外に `RestrictedApi` の抑止が増えていないことを確認する（`grep -rn 'RestrictedApi'`）

## 4. 回帰を確認する

- [ ] `./gradlew test :app:assembleDebug ktlintCheck detekt` が通る
- [ ] 実機をつないで `./gradlew :core:data:connectedDebugAndroidTest` が通る（DAO とマイグレーション。ADR-0018）
- [ ] **古い .rc を新しいプレイヤーで読めるか**: `docs/spikes/samples/*.rc` を新しい版で読み込み、`toNestedString()` のダンプと描画を確認する。読めなくなったファイルと原因を ADR に記録する
- [ ] 実機（Pixel 6a）でアプリ内再生とウィジェット表示を確認する
- [ ] スパイク記録（`docs/spikes/README.md`）の既知の落とし穴が直っていないか確認する（例: プレイヤーが 0x0 になる、`obtain()` が説明文を捨てる）。直っていれば回避策を外す

## 5. アルバムを移行する（ADR-0011）

- [ ] 描き方や生成結果が変わる場合は、Compiler の版（`:core:compiler` の `PLANT_COMPILER_VERSION`）を上げる。Remote Compose の版は Version Catalog から BuildConfig に入るので、手で書き換えるところはない（ADR-0018）
- [ ] アプリ起動時のマイグレーションで、メタデータの版が古い日が今の Compiler で作り直されることを確認する（logcat の `SieversiaCage: album: regenerated N day(s)`）
- [ ] 読込に失敗する日が残っていないことを確認する
- [ ] 新しい版で作った .rc を `docs/spikes/samples/`（または回帰用のサンプル置き場）に追加し、次回の版上げで「古い .rc」として使う

---

# Room と SQLite の版を上げるとき（ADR-0018）

アルバムの正のデータは Room 3 の DB にある。壊すと作り直しもできなくなるので、版上げも DB のスキーマ変更も慎重に行う。

## 版を上げる

- [ ] `gradle/libs.versions.toml` の `room3` と `sqlite` を更新する。Room の Gradle プラグインも同じ `room3` の版を使う
- [ ] リリースノートで、要求する Kotlin・KSP の版と、`BundledSQLiteDriver` まわりの変更を確認する
- [ ] `./gradlew test :app:assembleDebug ktlintCheck detekt` と、実機での `./gradlew :core:data:connectedDebugAndroidTest` が通る
- [ ] 実機で、版を上げる前に記録したアルバムがそのまま開けることを確認する（アプリを消さずに上書きインストールする）
- [ ] `:app:assembleRelease` の APK の大きさが大きく変わっていないか確認する（ADR-0018 の追記の値と比べる）

## スキーマを変えるとき（Sim の出力が増えたときなど）

- [ ] `SieversiaDatabase` の `version` を上げ、`core/data/schemas/` に書き出された新しい JSON をコミットする（古い版の JSON は消さない）
- [ ] マイグレーションを書き、`MigrationTestHelper` のテスト（`src/androidTest`）で、古い版で入れた行が新しい版でも正しく読めることを確かめる
- [ ] 1 から最新の版まで、すべてのマイグレーションを通すテストも足す
