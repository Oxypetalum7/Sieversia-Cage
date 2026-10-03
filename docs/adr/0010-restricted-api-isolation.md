# ADR-0010: RestrictTo の Remote Compose API を使い、依存箇所を閉じ込める

- ステータス: 採択
- 日付: 2026-10-03
- 関連: ADR-0003（alpha の追従は意識的なタスクにする）, ADR-0009

## 背景

alpha20 では、この作品に必要な API の大半が `@RestrictTo(LIBRARY_GROUP)` になっている。

- 作成側: `RemoteComposeWriter`、`RcPaint`、actions、プロファイル（ADR-0009）
- 再生側: `RemoteDocumentPlayer`、`RemoteComposePlayer`、`RemoteDocument`、`CoreDocument`
- ウィジェット用の補助クラス（`RemoteComposeWidget` / `RCWidget`）と、ウィジェット用プロファイル（`WIDGETS_V6` / `V7`）

RestrictTo は lint の `RestrictedApi` エラーになるだけで、コンパイルは通る。ただし API の安定性は保証されず、alpha が上がるたびに壊れる前提になる。一方、ウィジェットに載せる部分（`RemoteViews.DrawInstructions`）は公開 SDK の API である。

## 検討した選択肢

1. **RestrictTo を使わず、公開 API だけで作る** — 将来の互換性は最も高いが、プレイヤーがすべて RestrictTo なのでアプリ内再生（ショーケース・アルバム）が作れない。描画もほとんどできない
2. **RestrictTo を使い、lint の抑止をプロジェクト全体で有効にする** — 手軽だが、どこが RestrictTo に依存しているかが見えなくなる
3. **RestrictTo を使い、抑止する場所を限定する** — 依存箇所を特定のモジュールに閉じ込め、alpha を上げるときの影響範囲をはっきりさせる

## 決定

選択肢3。

- 作成側の RestrictTo は `:core:compiler` だけで使う。外には `PlantDocumentCompiler`（Sim の状態 → `ByteArray`）のような自前のインターフェイスだけを公開する
- 再生側の RestrictTo は、プレイヤーを包む自前の Composable（例: `PlantDocumentView(bytes)`）に閉じ込める。置き場所はスパイク中は `:app`、feature を切り出すときに `:core:player` のような専用モジュールにする
- 抑止はファイル単位の `@file:SuppressLint("RestrictedApi")` で行い、プロジェクト全体の lint 設定では無効にしない

## 理由

- 発注書の核である「ドキュメントを境界に、Sim と Surface を分離する」構造と一致する。RestrictTo への依存も、その境界の内側に閉じる
- alpha を上げるときに直す場所が、2つのモジュールに限られる

## 影響

- alpha を上げるたびに `:core:compiler` とプレイヤーを包むコードの修正が発生しうる（ADR-0003 の「追従は意識的なタスク」）
- 技術記事・README では「alpha20 時点では RestrictTo の API に頼らざるを得なかった」ことと、その閉じ込め方を説明する
