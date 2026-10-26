# ADR-0018: アルバムの正のデータは Room 3 に置き、.rc はファイルのキャッシュにする

- ステータス: 採択
- 日付: 2026-10-26
- 関連: ADR-0003（依存の版の固定）, ADR-0011（アルバムの正は成長データ）, ADR-0014（水やりの記録）, ADR-0015（Android 専用）, ADR-0016, ADR-0017, [KMP 向け SQLite（ドライバの実装）](https://developer.android.com/kotlin/multiplatform/sqlite#sqlite-driver-and-room)

## 背景

ADR-0011 で、アルバムの各日には `.rc` と一緒に「生成元の Sim 状態」と「生成メタデータ（Remote Compose の版、Compiler の版、プロファイル）」を保存し、版が古い日は作り直すと決めた。実装は `:core:data` を切り出すときに行うことにしていた（M1 タスク2）。

今の `AlbumStore` はスパイク④のままで、`day-N.rc` のバイト列を置くだけである。Sim 状態もメタデータも残しておらず、読み書きは画面からメインスレッドで呼んでいる。`Context` に依存しているため、JVM のテストも書けない。

決めることは次の 4 つ。

- 正のデータ（Sim 状態とメタデータ）をどこに、どの形で置くか
- Sim 状態として何を保存するか
- いつ作り直すか
- どうテストするか

## 検討した選択肢

### 保存の形

1. **日ごとの JSON + `.rc`**（`album/day-N/entry.json` と `document.rc`）— 依存が増えず、中身を adb で見やすい。形式の版の管理、書き込みの原子性、一覧のための走査を自前で書く。一覧は日数分のファイルを開く
2. **Room + `.rc`**（正のデータは DB、`.rc` はファイル）— スキーマの版、スキーマの書き出し、マイグレーション、そのテストの道具が揃っている。一覧や「版の古い日」の抽出が 1 回のクエリで済む。依存と設定が増える
3. **Proto DataStore + `.rc`** — 型は安全だが、protobuf の設定が増える。1 ファイルに全部入るので、日が増えるほど毎回まとめて読み書きする
4. **`.rc` も Room に BLOB で入れる** — 1 か所に収まるが、M3 で `.rc` が大きくなると DB が膨らみ、カーソルの大きさの上限も気になる。キャッシュを消すだけでも DB に触ることになる

### Sim 状態として保存するもの

1. **Sim の出力（`PlantState`）** — Sim の計算式を変えても、過去の日の育ち方は変わらない
2. **Sim の入力（経過日数・水やりの履歴・季節）** — 小さいが、Sim を改良すると過去の日の姿まで変わる
3. **両方** — どちらにも使えるが、形式の版の管理が二重になる

### SQLite のドライバ（Room 3 は SQLiteDriver で動く）

1. **`AndroidSQLiteDriver`**（`androidx.sqlite:sqlite-framework`）— 端末の OS に入っている SQLite を使う。ネイティブライブラリが増えない。SQLite の版は端末の OS の版で決まり、minSdk 29 の端末から最新の端末まで版がばらつく
2. **`BundledSQLiteDriver`**（`androidx.sqlite:sqlite-bundled`）— ソースからコンパイルした SQLite をアプリに同梱する。どの端末でも同じ最新の SQLite で動く。公式の推奨の実装で、Room のテストのページでも実機の DAO テストの例に使われている。ネイティブライブラリが ABI ごとに入る分、APK が大きくなる

## 決定

### 保存の形: Room 3 に正のデータ、`.rc` はファイル（選択肢2）

- Room は `androidx.room3` 3.0.3（2026-09-09 の安定版）を使う。ドライバは `BundledSQLiteDriver`（`androidx.sqlite:sqlite-bundled` 2.7.1）
- 表 `album_entry` を 1 つ作る。1 行が 1 日分

| 列 | 型 | 中身 |
| --- | --- | --- |
| `age_days` | INTEGER（主キー） | 植えてからの経過日数 |
| `stage` | TEXT | Sim の出力（`GrowthStage` の名前）。M2 で Sim の出力が増えたら列を足す |
| `remote_compose_version` | TEXT | 生成に使った Remote Compose の版 |
| `compiler_version` | INTEGER | 生成に使った Compiler の版（自前で採番） |
| `target` | TEXT | 再生先（`DocumentTarget` の名前。アルバムは `IN_APP`） |
| `created_at` | INTEGER | 生成した日時（エポックミリ秒） |

- Sim の出力は JSON の文字列にせず、列として持つ。形が変わったら Room のマイグレーションで追従し、形式の版の管理を Room の 1 本にまとめる
- `.rc` は `filesDir/album/day-N.rc` に置く。書き込みは一時ファイルに書いてからリネームする
- 保存の順番は「DB の行 → `.rc`」。途中で落ちても、正が DB に残っていれば `.rc` は作り直せる
- Entity と DAO は `:core:data` に置き、`PlantState` との変換も `:core:data` で行う。`:core:sim` には Room の注釈も serialization の注釈も付けない（ADR-0015 の純 Kotlin の壁）
- スパイク④で保存した `day-N.rc`（メタデータがない）は移行せず、捨てる

### Sim 状態: Sim の出力を保存する（選択肢1）

### 版の出どころ

- Compiler の版は `:core:compiler` の定数にし、描き方を変えたときに手で上げる（ADR-0011）
- Remote Compose の版は、Version Catalog の `remoteCompose` から `:core:compiler` の BuildConfig に生成する。ライブラリを更新したときに書き換え忘れが起きない

### 作り直し: 起動時 + 読込失敗時

- アプリの起動時に、バックグラウンドで次の日を作り直す
  - `compiler_version` が今の版より古い日
  - `remote_compose_version` が今の版と違う日
  - `.rc` がない日
- アルバムの画面で、プレイヤーが読込に失敗した日は、画面から `:core:data` に作り直しを頼む（`:core:data` は再生しないので、読めるかどうかはプレイヤーにしかわからない）
- 作り直す前の `.rc` のアーカイブは M1 では作らない（ADR-0011 で必須にしていない）

### テスト

| 何を確かめるか | どこで | 道具 |
| --- | --- | --- |
| Repository の考え方（作り直す日の選び方、保存の順番、読込失敗の扱い） | PC の JVM（`src/test`） | JUnit 5 + kotest。DAO と Compiler は偽物に差し替える |
| DAO の SQL | 実機（`src/androidTest`） | JUnit 4（`AndroidJUnit4`）。本番と同じ `BundledSQLiteDriver` |
| スキーマのマイグレーション（版 2 以降） | 実機（`src/androidTest`） | `MigrationTestHelper`（`androidx.room3:room3-testing`）。ドライバは本番と同じ `BundledSQLiteDriver` |

- スキーマは Room Gradle プラグイン（`androidx.room3`）で `core/data/schemas/` に書き出し、リポジトリに残す
- Robolectric は使わない（公式に非推奨）。PC の JVM で DB を動かす方法（`Context` なしの `inMemoryDatabaseBuilder`）は Room KMP の JVM ターゲットだけのもので、KMP を採らない `:core:data` では使えない

### ビルドの設定

- Room（KSP + `androidx.room3` プラグイン + `schemaDirectory`）は build-logic に convention plugin を足して当てる（ADR-0017）
- Android ライブラリの convention plugin に、`src/test` を JUnit 5 で動かす設定を足す。`:core:data` には `src/androidTest` の設定（`AndroidJUnitRunner`、androidx.test）を足す。androidx.test の版は実装のときに決め、Version Catalog に固定する（ADR-0003）

## 理由

- **構成が ADR-0011 の考え方をそのまま表す**: 正のデータは DB、作り直せるキャッシュはファイル、と置き場所で役割が分かれる
- **形式の版の管理を自前で書かずに済む**: ADR-0011 は「Sim 状態のシリアライズ形式は Remote Compose より長く読める必要があり、変えるときはマイグレーションを書く」としている。Room にはスキーマの版、書き出し、マイグレーション、そのテストの道具が揃っている
- **M2 以降の成長データも同じ DB に収まる**: 水やりの記録（ADR-0014）や株の進み具合は、表を足すだけで置ける
- **性能**: 1 日分は数百バイトで、数百日でも体感の差は出ない。それでも一覧と「版の古い日」の抽出が 1 回のクエリで済む分、Room が有利
- **Sim の出力を保存する**: ADR-0011 の「育ち方（成長データ）が正しく残ることを優先する」に合う。M2 で Sim を改良しても、過去の日の育ち方は変わらない
- **`BundledSQLiteDriver`**: Android 専用（ADR-0015）でも、OS の SQLite は端末の OS の版によって違う。同梱すれば、どの端末でも同じ SQLite で動く。DAO とマイグレーションのテストは手元の Pixel 6a でしか回さないので、テストした SQLite がそのまま全部の端末で動くことが、テストの意味を保つ決め手になる。公式の推奨の実装にも揃う

## 影響

- 依存が増える: `androidx.room3:room3-runtime` / `room3-compiler` / `room3-testing`（3.0.3）、`androidx.sqlite:sqlite-bundled`（2.7.1）、androidx.test 一式。ライブラリ更新チェックリスト（`docs/maintenance/library-upgrade.md`）に Room と SQLite を足す
- Room 3 の DAO は suspend 関数が前提なので、`:core:data` の公開 API も suspend にし、画面からはコルーチンで呼ぶ。仮の ViewModel を MVI に書き直す M1 タスク3 で、画面側の呼び方を整える
- DAO とマイグレーションのテストは実機が要り、CI ではすぐには回せない。当面は手元の Pixel 6a で回し、ライブラリ更新チェックリストに手順を足す
- `BundledSQLiteDriver` のネイティブライブラリの分だけ APK が大きくなる。どのくらい増えるかは公式のページに書かれていないので、実装のときに導入前後の APK の大きさを測り、この ADR に追記する
- Sim の出力の形が変わるたびに、Room のマイグレーションとそのテストを書くことになる
- スパイクで保存したアルバムの日は消える（実機の開発用データだけなので問題ない）
