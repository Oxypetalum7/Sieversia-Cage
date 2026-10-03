# ADR-0009: ドキュメント作成には低レベル API（RemoteComposeWriter）を使う

- ステータス: 採択
- 日付: 2026-10-03
- 関連: ADR-0004（モジュール構成）, ADR-0010（RestrictTo の扱い）, docs/spikes/README.md, [camaelon/remotecompose-experiments](https://github.com/camaelon/remotecompose-experiments)

## 背景

Remote Compose 1.0.0-alpha20 でドキュメントを作る方法は2通りある。

- **低レベル API**: `RemoteComposeWriter` / `RemoteComposeContext`（remote-creation-core / -android / -jvm）。描画命令を直接書き込む
- **Compose 風 API**: `captureSingleRemoteDocument { ... }`（remote-creation-compose）。`RemoteBox` や `RemoteCanvas` などの Composable を書くと、それがドキュメントに変換される

判断基準は、作品の価値に効く順に次の3つとした。

1. 描画の表現力: ベジェの茎や葉、花びらの補間、綿毛の回転、シェーダー
2. アダプティブレイアウト: 2x2 / 4x2 / 4x4
3. エッジでのインタラクション: 水やりのタップ

## 調査結果（2026-10-03、alpha20 のソースコードより）

| 観点 | 低レベル API | Compose 風 API |
| --- | --- | --- |
| パス（ベジェ）の描画 | ○ 座標に式を渡せる（`pathAppendCubicTo` など） | △ `drawPath` は RestrictTo。公開 API では `RemoteImageVector` 経由の回り道になる |
| 変形（rotate / translate / scale） | ○ 式を渡せる | △ Canvas の変形は RestrictTo。公開されているのは Modifier の `rotate` / `graphicsLayer` のみ |
| パスの補間（開花） | ○ `drawTweenPath`、`pathTween`、`addPathExpression` | × RestrictTo |
| 色の補間・グラデーション | ○ `addColorExpression`、線形・放射・円錐グラデーション | △ グラデーションの Brush は RestrictTo |
| 時刻の式 | ○ `Rc.Time.CONTINUOUS_SEC` と RPN 式 | △ 時刻の変数は RestrictTo（NaN エンコードを自前で書けば回避できる） |
| シェーダー | ○ `RemoteComposeShader` / `RcShaderScope` | × RestrictTo |
| レイアウト（行・列・`fitBox`・`stateLayout`） | ○ | ○ `RemoteFitBox` などが公開されていて書きやすい |
| タップでドキュメント内の状態を変える | ○ `ValueFloatExpressionChange` | ○ `clickable(valueChange(...))` |
| プロファイル（ウィジェット用など）の制御 | ○ 明示的に指定し、許可されていない op は書き込み時に例外になる | ○ 引数で指定 |
| 実行に必要なもの | 不要（core はプラットフォーム非依存。JVM 版もある） | Android の `Context` と Composition |
| API の公開状況 | ほぼすべて RestrictTo | レイアウトや状態は公開、描画の中核は RestrictTo |

## 検討した選択肢

1. **低レベル API を主に使う** — 描画の表現力が最も高い。どのみち RestrictTo は避けられない
2. **Compose 風 API を主に使う** — レイアウトは書きやすいが、植物の描画に必要な部分は結局 RestrictTo に頼るか、表現を諦めることになる
3. **併用（レイアウトは Compose 風 API、Canvas の中身は低レベル API）** — 1つのドキュメントの中で2つの API を混ぜる公開された方法が見当たらず、どちらの RestrictTo にも依存することになる

## 決定

選択肢1。`:core:compiler` の中で `RemoteComposeWriter` を薄い自前 DSL で包み、Sim の状態 → ドキュメントへの変換を書く。

## 理由

- 判断基準の筆頭である描画の表現力（パスの補間、式で動かす変形、シェーダー）がすべて揃っているのは低レベル API だけ
- RestrictTo への依存はどちらを選んでも避けられない。それなら、表現力が高く、依存先が一か所にまとまる方を選ぶ
- core がプラットフォーム非依存で JVM 版もあるため、Phase 2（remote-creation-jvm によるサーバー側生成）へ Compiler 層を移しやすい。Compose 風 API は Android の `Context` と Composition が前提になる
- 低レベル API にもレイアウト命令（`fitBox` や `stateLayout`）があり、2x2 / 4x2 / 4x4 の切り替えも書ける

## 影響

- Compose 風 API より記述量が増える。自前 DSL で読みやすさを補う
- RestrictTo の API を `:core:compiler` に閉じ込める方針が必要になる（ADR-0010）
- Phase 2 を見据えて、`:core:compiler` を Android library から JVM モジュール（remote-creation-core + -jvm）に変えられるかは、スパイク後に別の ADR で検討する
