# ADR-0016: スパイク後のモジュール構成（feature の切り出し）

- ステータス: 採択
- 日付: 2026-10-04
- 関連: ADR-0004（「スパイク後（予定）」を確定させる）, ADR-0010, ADR-0011, ADR-0013（M1）, ADR-0015

## 背景

ADR-0004 では、feature の切り出しをスパイクの結果を見てから行うことにしていた。スパイクでは、ウィジェットが主役として成立することがわかった（退路は使わない）。また、再生側の RestrictTo は showcase とアルバムの両方が使うことがわかった。M1（ADR-0013）の最初のタスクとして、モジュール構成を確定させる。

## 検討した選択肢

1. **ADR-0004 の予定どおり**（`:feature:showcase` / `:feature:album` / `:feature:widget` / `:core:data`）— 再生を包むコードの置き場所が決まっていない。どちらかの feature に置くと、もう一方の feature がそれに依存してしまう
2. **ADR-0004 の予定に `:core:player` を足す** — 再生側の RestrictTo を1つの core モジュールに閉じ込め、showcase とアルバムの両方から使う

## 決定

選択肢2。

```
:app                     起動・画面遷移（Navigation Compose）・Hilt の集約だけ
 ├─ :feature:showcase    今日の株を再生する画面
 ├─ :feature:album       日ごとの一覧と再生
 └─ :feature:widget      ホーム画面ウィジェット
:core:player             再生側の RestrictTo を閉じ込める（showcase / album が使う）
:core:data               成長データ・アルバムの保存と、ドキュメント生成の窓口（3 つの feature が使う）
:core:compiler           作成側の RestrictTo を閉じ込める
:core:sim                純 Kotlin
```

- feature 同士は依存しない。画面遷移は `:app` が持ち、各 feature は画面の Composable と遷移先の定義だけを公開する
- feature は `:core:compiler` を直接使わない。「どの日を、どの再生先（`DocumentTarget`）向けに」作るかは `:core:data` の窓口を通す
- `:core:model` と `:core:designsystem` はまだ作らない。成長データ（`PlantState`）は `:core:sim` で足りている。必要になったら切り出す

## 理由

- 再生側の RestrictTo を `:core:player` に、作成側を `:core:compiler` に閉じ込めると、alpha を上げるときに直す場所がこの2つに限られる（ADR-0010 の予定どおり）
- ドキュメントの生成を `:core:data` に集めると、ADR-0011 の作り直し（メタデータを見て `.rc` を再生成する）を1か所で扱える。ウィジェットとアルバムで生成の仕方がずれない

## 影響

- `:app` のスパイクのコードを、次のように移す
  - `PlantDocumentView.kt`（`dumpDocument` を含む）→ `:core:player`
  - `PlantWidgetProvider.kt`・ウィジェットのリソース・マニフェストの `<receiver>` → `:feature:widget`
  - `SpikeAlbumStore.kt` → `:core:data`（中身の作り直しは M1 のタスク2）
  - `MainActivity.kt` の `SpikeScreen` → 再生部分は `:feature:showcase`、アルバムは `:feature:album`
- detekt の `ignoreFailures` を外し、違反でビルドが落ちるようにする（ADR-0005）
- MVI での書き直しと CI は、M1 の後のタスクで行う。切り出しの時点では、画面の中身はスパイクのコードを移すだけにする
