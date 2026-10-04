# 初日スパイク

発注書「初日スパイク」の検証項目と結果の記録。判断を伴う結果は ADR に起こす。

| # | 検証項目 | 状態 | 結果・メモ |
| --- | --- | --- | --- |
| ① | アプリ内再生（最小ドキュメントの生成 → 再生） | **成功**（2026-10-03） | 下記「① 結果」参照 |
| ② | 手元端末のランチャーでの次世代ウィジェット表示 | **成功**（2026-10-04） | 下記「② 結果」参照 |
| ③ | 式でどこまでアニメーションが書けるか（揺れ・回転・開花の補間・水やりタップ） | **成功**（2026-10-04） | 下記「③ 結果」参照 |
| ④ | ドキュメントのファイル保存 → 再読込 | **成功**（2026-10-04） | 下記「④ 結果」参照 |
| ⑤ | AGSL（シェーダー）をどこまで使えるか | **アプリ内は成功**（2026-10-04） | 下記「⑤ 結果」参照。ウィジェットでは使えない見込み（②で確認） |

進める順番: ① → ④ → ③ → ⑤ → ②（①〜④で作るドキュメントを⑤と②で使い回すため）

## 使うツール

- **RemoteCompose Inspector**（https://camaelon.github.io/remotecompose-experiments/inspector/）: .rc ファイルをブラウザで解析する。命令の一覧、変数のスライダー（`$TIME` など）、式の依存関係のグラフ、再描画の原因、プロファイラーを確認できる
- **RemoteCompose Spec**（https://camaelon.github.io/remotecompose-experiments/RemoteComposeSpec/remote_compose_spec.html）: 命令ごとの仕様
- 出典: [camaelon/remotecompose-experiments](https://github.com/camaelon/remotecompose-experiments)（Apache 2.0）。Remote Compose の開発者による実験用リポジトリで、Google の公式ではない。中のプレイヤーは独自の移植なので、最終的な判断は androidx の `remote-core`（実機）で行う
- 既知の罠（同リポジトリの `STATUS.md` / `docs/MISSING_SUPPORT.md` より）:
  - 値が黙って 0 になる失敗が多い。シェーダーの指定が NaN になると、エラーを出さずにシェーダーなしで描かれる
  - `DrawTextOnCircle` は androidx の本家でも命令として登録されていないので使わない
- Inspector で確かめたこと（2026-10-04、`samples/spike1-day10.rc`）:
  - 描画され、時刻の式によるアニメーションも動いた。Disassembly は端末の `toNestedString()` と一致（25 命令、377 バイト）
  - 時刻のスライダーは DAG パネルの「Live Simulator」にある。名前は `$TIME` ではなく `CONTINUOUS_SEC`。動かすと茎の揺れが連動した
  - 地面（`#6D5A4B`）が Inspector でだけ灰色っぽく見える。実機では茶色。Inspector のプレイヤーは独自の移植なので、色の最終判断は実機で行う

## ① 結果（2026-10-03）

Sim（`simulate(ageDays)`）→ `WriterPlantDocumentCompiler`（`RemoteComposeWriter`、プロファイル `ANDROIDX`）→ `RemoteDocumentPlayer` の流れが Pixel 6a で通った。

- ドキュメントは地面・茎・花の最小構成で **377 バイト**
- 茎は `sin(CONTINUOUS_SEC * 2) * 5` 度で根元を支点に揺れ、プレイヤー側で時刻の式が評価されることを確認（③の入口）
- 座標は `addComponentWidthValue()` / `addComponentHeightValue()` を使った相対値で書いた

### 落とし穴: プレイヤーが 0x0 になる

- `root` を持つドキュメントでは、`CoreDocument.getWidth()` / `getHeight()` がレイアウト前のルートの大きさ（0）を返す。そのまま `RemoteDocumentPlayer(documentWidth = document.width, ...)` に渡すと何も表示されない
- さらに、作成側のサイズを `documentWidth` / `documentHeight` に渡しても、`ViewFactoryHolder` が 0x0 のままだった（ソース上は内部で `Modifier.size(documentWidth.dp, ...)` を付けているが、効いていない。原因は未特定）
- 回避策: 呼び出し側の `modifier` に `Modifier.size(...)` を明示的に付ける（`PlantDocumentView.kt`）
- 調べ方: `adb shell dumpsys activity top` で View 階層の大きさを見ると早い

## ④ 結果（2026-10-04）

- 生成したバイト列を `files/album/day-N.rc` に保存し、読み直したものを再生した。11 日分すべてで元のバイト列と完全一致（`contentEquals`）
- アプリを完全に終了（`am force-stop`）して再起動したあと、アルバムから Day 10 をファイルだけで再生できた
- `RemoteDocument(bytes).document.toNestedString()` で命令を入れ子のまま出せる。式は RPN で記録される（例: 揺れは `FloatExpression[47] = ([1] 2.0 * sin 5.0 * )`、`[1]` は `CONTINUOUS_SEC`）。`toString()` はヘッダーとルートしか出さないので使わない
- サンプル: `samples/spike1-day0.rc` / `samples/spike1-day10.rc`（377 バイト）。先頭は HEADER 命令と `0x048C0000 \| 1`

### 落とし穴: ドキュメントの説明文が入らない

- `RemoteComposeWriter.obtain(w, h, contentDescription, profile)` は説明文を捨てる。`RcPlatformProfiles` のファクトリーがすべて `null` を渡すため
- 説明文を受け取るコンストラクタを直接呼ぶと、プロファイル専用の Writer（例: `WIDGETS_V6` の `WidgetsProfileWriterV6`）を経由しなくなる
- ウィジェットのアクセシビリティは、本実装でコンポーネント単位の semantics で対応する

### 本実装への申し送り

- 前方互換の保証がない（未知の命令があると読込に失敗する）ため、アルバムには `.rc` と一緒に生成元の Sim 状態も保存し、ライブラリ更新後に作り直せるようにする（`:core:data`）

## ③ 結果（2026-10-04）

4 つとも、Pixel 6a のアプリ内再生で動いた。サンプル: `samples/spike3-day7.rc`（つぼみ）/ `spike3-day10.rc`（開花）/ `spike3-day14.rc`（綿毛）。

| 動き | 書き方 | 確認方法 |
| --- | --- | --- |
| 揺れ | `sin(CONTINUOUS_SEC * 2π·1146/3600) * 5` 度で `rotate` | 2 秒あけたスクリーンショットで傾きが変わる |
| 綿毛の回転 | `CONTINUOUS_SEC * 90` 度で `rotate` | 0.5 秒あけたスクリーンショットで線の角度が変わる |
| 開花の補間 | 同じコマンド列の2つのパス（つぼみ・花）を `drawTweenPath` で補間。`tween` は 8 秒周期の式 | 開いた花とつぼみの両方の状態が撮れた |
| 水やりタップ | Canvas に `onClick(ValueFloatExpressionChange(lastTap, CONTINUOUS_SEC))`。茎を `1 + sin(clamp((t - lastTap) / 0.6, 0, 1) · π) · 0.15` 倍に縦に伸ばす | 花の中心が静止時 y≈302 → タップ直後 270〜285（px）に持ち上がり、また戻る |

- タップ時の式はプレイヤーがその瞬間に評価して変数を上書きする（`CoreDocument.evaluateFloatExpression` → `overrideFloat`）。「タップした時刻を覚えて、そこから式で動かす」が作成側だけで書ける
- `addNamedFloat("lastTap", ...)` で名前を付けておくと、ダンプ（`VariableName[42] = "lastTap"`）や Inspector で追いやすい
- ①のドキュメントは 377 バイト、③は 828〜1098 バイト。パス2本（各 5 本のベジェ）を足しても 1KB 前後

### 毎正時の継ぎ目

`CONTINUOUS_SEC` は毎正時に 3600 → 0 へ戻る。周期で 3600 秒が割り切れないと、正時に絵が跳ぶ。

- ①の `sin(t * 2)` は周期 π 秒で割り切れなかった。角速度を `2π × 1146 / 3600`（≒ 2.00015 rad/s）にして、1 時間にちょうど 1146 往復にした
- 綿毛は 90 度/秒 × 3600 秒 = 900 回転、開花は 8 秒周期 × 450 回で、どちらも割り切れる
- 正時をまたいだタップは `t - lastTap` が負になり、跳ねずに終わる。0.6 秒の範囲なので許容した
- 実機で正時を待っての確認はしていない。Inspector の `CONTINUOUS_SEC` スライダーで 3599 → 0 を動かすと確かめられる

### 落とし穴: Canvas の座標は実機のピクセル

- `addComponentWidthValue()` は実機のピクセル（Pixel 6a で 840）を返す。基準サイズ 320 のつもりで書いた数値（花びらの長さ 30 など）は、そのままだと 1/2.6 の大きさになる
- 花の部分は `scale(width / 320)` をかけてから、基準サイズの座標で描いた。茎の太さ（`StrokeWidth(6.0)`）はピクセルのままなので、本実装では線の太さも同じ単位にそろえる
- `rotate(angle)`（中心なし）はダンプ上 `MatrixRotate [52] NaN NaN` になるが、原点まわりに回った

### 落とし穴: 操作がないと平均 10fps に間引かれる

- 症状: 起動直後とタップ直後は滑らかだが、放っておくと数秒でカクつく
- 原因: プレイヤー（`RemoteComposeView` の `Limiter`）は、瞬間の上限 60fps に加えて「直近 10 秒の平均 10fps」を上限にしている（`Limits.DEFAULT_MAX_AVG_FPS = 10` / `DEFAULT_WINDOW_SEC = 10`）。10 秒で 100 フレームを使い切ると間引かれる。タッチすると `touchBoost()` で履歴が消え、また滑らかになる
- 計測（`adb shell dumpsys gfxinfo`、起動から 15 秒放置したあとの 10 秒間）: 修正前 102 フレーム（≈10fps）→ 修正後 604 フレーム（≈60fps）。どちらも Janky frames は 0%。処理が重いのではなく、意図的な間引き
- 対処: `RemoteDocumentPlayer` の `update` で `setMaxAvgFps(60)` を呼ぶ（`PlantDocumentView.kt`）。`setDocument` のたびに既定値へ戻るため、`init` では効かない。`update` は `setDocument` の後に呼ばれる
- `RemoteComposePlayer`（View 版）のクラスを参照するため、`remote-player-view` を `:app` の依存に足した
- ヘッダーの `DOC_DESIRED_FPS` は瞬間の上限しか変えず、平均の上限はドキュメント側からは変えられない
- **②への申し送り**: ホーム画面のウィジェットは OS 内蔵のプレイヤーが再生するので、アプリからこの上限を変えられない。同じ間引きがあるなら、ウィジェットでの常時アニメーションは 10fps 前後になる前提で、ゆっくりした動きにする。②で実測する
  - SDK 同梱の android-36 ソース（rev 1、内蔵プレイヤーは API レベル 4）の `RemoteComposeCanvas` には瞬間の上限（60fps）しかなく、平均の上限（`Limiter`）はない
  - ただし実機の内蔵プレイヤーは alpha20 と同世代（API レベル 8）なので、`Limiter` が入っている可能性がある。ランチャー側で間引いている可能性もある
  - 測り方: ウィジェットを置いて放置し、ランチャーのプロセスに `adb shell dumpsys gfxinfo <ランチャーのパッケージ>` を使う

### 落とし穴（スパイクのコード）: 同じ日を上書きしても再描画されない

- 保存済みの日の一覧（`savedDays`）が変わらないと再コンポーズが起きず、画面には古いファイルの内容が残っていた。保存のたびに数を進め、`remember` のキーにして読み直すようにした（`MainActivity.kt`）

## ② 結果（2026-10-04）

Pixel 6a（Pixel Launcher、内蔵プレイヤーは API レベル 8）のホーム画面に、Day 10 のドキュメントをウィジェットとして載せた。`WIDGETS_V6` と `WIDGETS_V7` の両方で試した。

- 載せ方: `AppWidgetProvider.onUpdate` で `WriterPlantDocumentCompiler(DocumentTarget.WIDGET_V6)` でコンパイルし、`RemoteViews(RemoteViews.DrawInstructions.Builder(listOf(bytes)).build())` を渡す（`PlantWidgetProvider.kt`）。公開 SDK の API だけで済み、RestrictTo は使わない。API 36 未満では `initialLayout` の文字だけの表示のまま
- プロファイル（RestrictTo）は `:core:compiler` の外に出さず、`DocumentTarget`（`IN_APP` / `WIDGET_V6` / `WIDGET_V7`）で選ぶ（ADR-0010）
- ホスト側（`RemoteViews.SetDrawInstructionAction.apply`）は、ランチャーの中で `RemoteComposePlayer` を作って `setDocument` するだけ。`setShaderControl` も平均フレームレートの設定もしていない

| 確認したこと | WIDGETS_V6（1641 バイト） | WIDGETS_V7（1130 バイト） |
| --- | --- | --- |
| 表示（空・地面・茎・花） | ○ | ○ |
| 時刻の式（揺れ）・開花の補間（`drawTweenPath`） | ○ 2 枚でつぼみと開いた花が撮れた | ○ |
| シェーダー | 外されて paint の単色（`SKY`）になった。⑤の見込みどおり | 書き込まない（V7 は `DATA_SHADER` を書けない） |
| 放置中のフレーム数（`dumpsys gfxinfo` をランチャーに。15 秒放置後の 10 秒間） | 603（≈60fps）、Janky 0% | 605（≈60fps）、Janky 0% |
| タップ（水やり）で跳ねる | ○ 花の中心 y=186.5 → 172〜181 | ○ y=186.7 → 169〜171 |

わかったこと:

- **フレームレートの間引きはなかった**。アプリ内のプレイヤーにある「直近 10 秒の平均 10fps」の制限は、ランチャーの内蔵プレイヤーではかかっていない（60fps で回り続ける）。②への申し送りで心配していた「ウィジェットでは 10fps 前後」は当たらなかった。逆に、ウィジェットは放っておいても 60fps で描き続けるので、電池への影響は本実装で気にする（動きを止める時間帯を作る、など）
- **シェーダーは外される**。V6 なら書けるが、内蔵プレイヤーが既定で拒否するので単色になる。ウィジェットの見た目はシェーダーなしで成り立たせる
- **ドキュメント内のタップ（`ValueFloatExpressionChange`）はランチャーでも効く**。アプリを起こさずに、ドキュメントの中だけで水やりの反応が返せる。アプリに水やりを記録する（Sim に伝える）には、別にホストへのアクション（`SetOnClickResponse` 経由の Intent）が要る。本実装で確かめる
- 花の大きさは幅に比例した（ウィジェットの幅 470px、アプリ内は 840px）。「幅 ÷ 320」倍の拡大は内蔵プレイヤーでも効く
- レベル 8 の内蔵プレイヤーは、V6（ヘッダー `v1.0.0` の古い形式）も V7 も再生できた
- プロファイルの選択は ADR-0012 に起こした（採択）

まだ確かめていないこと:

- 4x2 / 4x4 にリサイズしたときの見え方（今のドキュメントは正方形の前提で、横長では茎が中央に 1 本だけになる）
- ランチャーを離れて戻ったとき、画面が消えて点いたときに、アニメーションが続くか
- `obtain` 経由では `RootContentBehavior`（枠いっぱいに拡大縮小）が入らない。今は Canvas が `fillMaxSize` で幅と高さを読んでいるので問題になっていない

## ② 事前確認（2026-10-03）

Pixel 6a（Android 16 / API 36、`CP1A.260405.005`、Pixel Launcher）で `RemoteViews.DrawInstructions.getSupportedVersion()` を呼んだ結果は **8**。

- この関数は OS 内蔵プレイヤーの `CoreDocument.getDocumentApiLevel()` を返す（`sources/android-36/android/widget/RemoteViews.java`）
- SDK 同梱の android-36 ソース（rev 1）では内蔵プレイヤーは API レベル 4 だったが、実機の OS は更新されていて **alpha20（`DOCUMENT_API_LEVEL = 8`）と同世代**
- 「1.x のドキュメントは内蔵プレイヤーに弾かれる」という懸念はひとまず解消。実際に描画されるか、タップが届くか、アニメーションが続くかは本番の②で確認する

## ⑤ 結果（2026-10-04）

空を AGSL で描いた（上が青く下が明るいグラデーションに、斜めの細い光の帯が 10 秒周期で右へ流れる）。サンプル: `samples/spike5-day0.rc`。

- 作成側: `w.createShader(src).setFloatUniform(...).commit()` で ID を得て、`rcPaint.setShader(id)` で使う。uniform には式（NaN の変数）を渡せる。位相は作成側の式（`CONTINUOUS_SEC * 2π·360/3600`）で作って渡し、毎正時の継ぎ目の調整をシェーダーに持ち込まない
- 描いたあとは `rcPaint.setShader(0)` で外す（プレイヤーは 0 で `setShader(null)` にする）
- 最初は帯を `0.06 × (0.5 + 0.5 sin)` の明るさで 20 秒周期にしたが、実機で見て帯がわからなかった（同じ 1 点の RGB が 20 秒かけて 13 段階ほど変わるだけ）。`pow(..., 6)` で細い筋にし、明るさを 0.25、周期を 10 秒にしてはっきり見えるようにした。見た目の作り込みは本実装で行う
- 負荷（起動から 15 秒放置後の 10 秒間、`dumpsys gfxinfo`）: 604 フレーム、Janky 0%、99 パーセンタイル 13ms。プレイヤーは paint を適用するたびに `new RuntimeShader(...)` を作っているが、この規模では問題にならなかった

### 落とし穴: プレイヤーは既定ですべてのシェーダーを拒否する

- 症状: ドキュメントには `SHADER DATA` も `Shader(51)` も入っているのに、空が単色のまま。エラーもログも出ない
- 原因: `RemoteComposePlayer`（View 版）の `ShaderControl` の既定値が `(shader) -> false`。`setDocument` の中の `checkShaders` で、許可されなかったシェーダーは無効になる（`ShaderData.mShaderValid = false` のまま読み込まれない）
- 対処: `RemoteDocumentPlayer` の `init` で `setShaderControl` を設定する（`checkShaders` は `setDocument` の中で走るので、`update` では遅い）。許可するのは、コンパイラが書き込むシェーダー（`PLANT_DOCUMENT_SHADERS`）と文字列が完全に一致するものだけにした。アルバムのファイルが差し替えられても、知らないシェーダーは動かさない
- 完全一致なので、シェーダーを書き換えると、それより前に保存したアルバムの日は空が単色になる（古いシェーダーが拒否される）。アルバムを成長データから作り直す前提（ADR-0011）と合わせて、シェーダーの変更もライブラリ更新と同じく「作り直しの理由」として扱う
- さらに API 33 未満では許可しない。プレイヤーの `AndroidPaintContext.setShader` は API を確かめずに `RuntimeShader`（API 33〜）を作るため、許可するとクラッシュするおそれがある。許可しなければ paint の色（`SKY`）の単色で描かれる。この見た目は、許可を入れる前の実機で確認済み

### ウィジェット用プロファイル

同じドキュメントを各プロファイルで書き込んだ結果（使い捨てのプローブで確認。プローブはコミットしていない）:

| プロファイル | 結果 |
| --- | --- |
| `ANDROIDX` | 書ける（1653 バイト） |
| `WIDGETS_V6` | 書ける（1631 バイト、ヘッダー v1.0.0）。API レベル 6 の命令セットに `DATA_SHADER` が入っている |
| `WIDGETS_V7` | 書き込み時に例外: `Operation 45 is not supported for this version`（45 = `DATA_SHADER`） |

- ただし、書けても動くとは限らない。SDK 同梱の android-36 ソースでは、OS 内蔵プレイヤーの `ShaderControl` も既定で `false`（「The default is to not accept shaders」）で、`setShaderControl` を呼んでいる箇所がない。ウィジェットではシェーダーが外され、paint の単色になる見込み
- **②への申し送り**: ウィジェットの見た目はシェーダーなしで成立させる（paint の色を、シェーダーがないときの見た目として選ぶ）。実機の内蔵プレイヤー（API レベル 8）で本当に外されるかは②で確かめる

### WIDGETS_V6 と WIDGETS_V7 の違い（alpha20 のソースより、2026-10-04）

| | `WIDGETS_V6` | `WIDGETS_V7` |
| --- | --- | --- |
| API レベル | 6 | 7 |
| プロファイルのフラグ | `0`（プロファイルの考え方がまだない） | `PROFILE_WIDGETS` |
| Writer | `WidgetsProfileWriterV6`（専用） | `RemoteComposeWriterAndroid`（`ANDROIDX` と同じ） |
| 位置づけ | ソースのコメントは「Profile for Glance Widgets for Platform 16」（Android 16 / Baklava） | V7 以降の、命令セットをプロファイルで分ける方式 |
| ヘッダー | `v1.0.0` の古い形式（⑤のプローブで確認） | （⑤では `DATA_SHADER` の例外で書き込みまで進まなかった） |
| 再生できるプレイヤー | API レベル 6 以上 | API レベル 7 以上 |

命令セットの組み立て方（`Operations.java`）:

- V6: 基本の命令セット（`fillDefaultVersionMap`）に `DATA_SHADER` と `ROOT_CONTENT_BEHAVIOR` を足しただけ
- V7 以降: 基本の命令セット + プロファイルごとの追加分 + V7 共通の命令（`REM`、`MATRIX_EXPRESSION` などの行列演算）
  - `PROFILE_WIDGETS` の追加分（17 個）: `MATRIX_FROM_PATH`、ビットマップフォントの文字描画、`DRAW_TO_BITMAP`、`WAKE_IN`、`ID_LOOKUP`、`PATH_EXPRESSION`、動的な数値リスト、`CORE_TEXT` / `TEXT_STYLE` / `TEXT_TRANSFORM`、`COLOR_THEME` など
  - `DATA_SHADER` は基本の命令セットにもウィジェット用の追加分にもなく、`ANDROIDX` 用の追加分にだけある。V6 にあったシェーダーが、V7 ではアプリ内プレイヤー専用に移された形
  - `ROOT_CONTENT_BEHAVIOR` は、ウィジェット用では「非推奨」の追加分に移った
  - 複数のプロファイルを指定すると、すべてに共通する命令だけが使える（どのプレイヤーでも動くことを保証するため）

`WidgetsProfileWriterV6` が書き込み時に追加で制限していること:

- 式の演算子を API レベル 6 のもの（`OFFSET + 50` 未満）に制限する。`floatExpression` のたびに `validateOps` で検証し、範囲外なら例外。V7 で増えた演算子（`SMOOTH_STEP`、`FRACT`、`PINGPONG`、`CUBIC`、`LOG2`、レジスタ操作、配列の集計など）は使えない
- カスタムフォントを追加できない。画像の透明度と文字の大きさに式（NaN）を使えない
- `createWriter` 経由のときだけ、ルートを枠いっぱいに拡大縮小する `RootContentBehavior` を入れる（`obtain` 経由では入らない）

今のドキュメント（①③⑤）との関係:

- ⑤のプローブで V6 の書き込みが通ったので、今の式（sin・cos・clamp など）はすべて API レベル 6 の範囲に収まっている
- シェーダーは V6 なら書けるが、OS 内蔵プレイヤーが既定で拒否する見込み（上記）なので、どちらのプロファイルを選んでもウィジェットでは使わない
- まだ確かめていないこと: レベル 8 の内蔵プレイヤーが V6 のドキュメントを再生できるか（新しいプレイヤーが古いドキュメントを読めるか）。②で両方のプロファイルを試す

## ⑤ 検証観点

- 作成側: 低レベル API（`RemoteComposeShader` / `RcShaderScope`）でシェーダーを書けるか。uniform に時刻の式を渡してアニメーションさせられるか
- アプリ内: API 33 未満では単色にフォールバックする（`RcPlayerPaint.kt`）。minSdk 29 なので、シェーダーなしでも成立する見た目にしておく
- ウィジェット: androidx のウィジェット用プロファイルは `DATA_SHADER` を許可していない。一方、OS 内蔵プレイヤーの op セットには `DATA_SHADER` がある。プロファイルの指定次第でホーム画面でも動くのかを確かめる
