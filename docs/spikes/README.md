# 初日スパイク

発注書「初日スパイク」の検証項目と結果の記録。判断を伴う結果は ADR に起こす。

| # | 検証項目 | 状態 | 結果・メモ |
| --- | --- | --- | --- |
| ① | アプリ内再生（最小ドキュメントの生成 → 再生） | 未着手 | ADR-0009 の作成 API で実施 |
| ② | 手元端末のランチャーでの次世代ウィジェット表示 | 事前確認のみ | 下記「② 事前確認」参照 |
| ③ | 式でどこまでアニメーションが書けるか（揺れ・回転・開花の補間） | 未着手 | |
| ④ | ドキュメントのファイル保存 → 再読込 | 未着手 | ソース上はバイト列単体で完結。ただし前方互換の保証はなく、未知 op で読込失敗する |
| ⑤ | AGSL（シェーダー）をどこまで使えるか | 未着手 | 2026-10-03 追加。下記「⑤ 検証観点」参照 |

進める順番: ① → ④ → ③ → ⑤ → ②（①〜④で作るドキュメントを⑤と②で使い回すため）

## 使うツール

- **RemoteCompose Inspector**（https://camaelon.github.io/remotecompose-experiments/inspector/）: .rc ファイルをブラウザで解析する。命令の一覧、変数のスライダー（`$TIME` など）、式の依存関係のグラフ、再描画の原因、プロファイラーを確認できる
- **RemoteCompose Spec**（https://camaelon.github.io/remotecompose-experiments/RemoteComposeSpec/remote_compose_spec.html）: 命令ごとの仕様
- 出典: [camaelon/remotecompose-experiments](https://github.com/camaelon/remotecompose-experiments)（Apache 2.0）。Remote Compose の開発者による実験用リポジトリで、Google の公式ではない。中のプレイヤーは独自の移植なので、最終的な判断は androidx の `remote-core`（実機）で行う
- 既知の罠（同リポジトリの `STATUS.md` / `docs/MISSING_SUPPORT.md` より）:
  - 値が黙って 0 になる失敗が多い。シェーダーの指定が NaN になると、エラーを出さずにシェーダーなしで描かれる
  - `DrawTextOnCircle` は androidx の本家でも命令として登録されていないので使わない

## ② 事前確認（2026-10-03）

Pixel 6a（Android 16 / API 36、`CP1A.260405.005`、Pixel Launcher）で `RemoteViews.DrawInstructions.getSupportedVersion()` を呼んだ結果は **8**。

- この関数は OS 内蔵プレイヤーの `CoreDocument.getDocumentApiLevel()` を返す（`sources/android-36/android/widget/RemoteViews.java`）
- SDK 同梱の android-36 ソース（rev 1）では内蔵プレイヤーは API レベル 4 だったが、実機の OS は更新されていて **alpha20（`DOCUMENT_API_LEVEL = 8`）と同世代**
- 「1.x のドキュメントは内蔵プレイヤーに弾かれる」という懸念はひとまず解消。実際に描画されるか、タップが届くか、アニメーションが続くかは本番の②で確認する

## ⑤ 検証観点

- 作成側: 低レベル API（`RemoteComposeShader` / `RcShaderScope`）でシェーダーを書けるか。uniform に時刻の式を渡してアニメーションさせられるか
- アプリ内: API 33 未満では単色にフォールバックする（`RcPlayerPaint.kt`）。minSdk 29 なので、シェーダーなしでも成立する見た目にしておく
- ウィジェット: androidx のウィジェット用プロファイルは `DATA_SHADER` を許可していない。一方、OS 内蔵プレイヤーの op セットには `DATA_SHADER` がある。プロファイルの指定次第でホーム画面でも動くのかを確かめる
