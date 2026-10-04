# ADR-0005: 開発標準の導入タイミング

- ステータス: 採択（DI の行は ADR-0015 により Hilt に変更）
- 日付: 2026-10-03
- 関連: ADR-0004

## 背景

開発者の標準規約では、Koin（DI）、MVI、Arrow-kt、ktlint + detekt（compose-rules 付き）、JUnit5 + MockK + Turbine、Version Catalog の利用を定めている。初日スパイクは「構想の背骨が通るか」を短時間で検証するのが目的で、すべてを初日に入れると検証が遅れる。どれをいつ入れるかを決める。

## 検討した選択肢

1. **すべて初日から導入** — 後から入れ直す手間はないが、スパイク中の試行錯誤コードにも規約がかかり、検証が遅くなる
2. **すべてスパイク後に導入** — スパイクは最速だが、後から ktlint / detekt を入れると大量の差分が出る
3. **「後から入れると高くつくもの」だけ初日に入れる**

## 決定

選択肢3。

| 項目 | 導入時期 | 理由 |
| --- | --- | --- |
| Version Catalog | 初日 | ADR-0003 の前提 |
| ktlint | 初日 | 後から入れると全ファイルに差分が出る |
| detekt + compose-rules | 初日（ただし CI で落とすのは feature 切り出し後） | 設計の臭いは早めに見たい。スパイク中は警告止まり |
| JUnit5 + Kotest assertions | 初日（`:core:sim` のみ） | Sim は純関数中心で、テストを書くコストが低く価値が高い |
| Arrow-kt | `:core:sim` / `:core:compiler` で必要になった時点 | Either / Option の出番がスパイクでは少ない |
| Koin + verify テスト | feature 切り出し時 | スパイクの `:app` 一枚では DI の価値が薄い |
| MVI（UiState / Intent / Effect） | feature 切り出し時 | 画面はショーケースとアルバムだけで、スパイク中は使い捨ての UI |
| MockK / Turbine | ViewModel や Flow が登場した時点 | 同上 |

## 理由

- 後付けの差分コストが大きいもの（フォーマット、カタログ、Sim のテスト）だけ先に払う
- DI や MVI は画面構成が決まってから入れた方が設計がぶれない

## 影響

- feature 切り出しのタスクに「Koin / MVI / detekt の CI ゲート導入」を含める
- スパイク中の `:app` のコードは規約の一部を満たさない。feature 切り出しで書き直す前提とする
