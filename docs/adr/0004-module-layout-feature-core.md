# ADR-0004: モジュール命名を feature/core 規約に揃え、段階的に分割する

- ステータス: 採択
- 日付: 2026-10-03
- 関連: ADR-0002（構成を具体化する。廃止はしない）

## 背景

ADR-0002 で `:sim` / `:compiler` / `:app` の三層構成を採択した。一方、開発者の標準規約では「`:app` + `:feature:xxx` + `:core:xxx` で最初から分割し、feature 間の直接依存は禁止」としている。三層の分離思想（Sim と Surface はドキュメントだけで繋がる）を保ったまま、命名と分割のタイミングを規約に合わせる必要がある。

## 検討した選択肢

1. **ADR-0002 の名前のまま（`:sim` / `:compiler` / `:app`）** — スパイクは最速だが、規約から外れ、後から改名が必要になる
2. **初日から feature/core を全部切る** — 規約どおりだが、スパイク段階では中身の無いモジュールが増え、検証の速度が落ちる
3. **core 側は初日から規約名で切り、feature はスパイク後に切り出す** — 背骨（Sim → Compiler）は最初から規約どおり。Surface はスパイクの結果（ウィジェットが主役か、退路か）を見てから分割する

## 決定

選択肢3。

スパイク段階:

```
:core:sim        純Kotlin JVM。成長モデル（ADR-0002 の :sim）
:core:compiler   Android library。Sim状態 → RemoteCompose ドキュメント（ADR-0002 の :compiler）
:app             スパイク用の画面とウィジェットを一時的にここに置く
```

スパイク後（予定）:

```
:feature:showcase   remote-player-compose による再生
:feature:album      成長アルバム
:feature:widget     ホーム画面ウィジェット
:core:data          ドキュメントの保存・読込、Sim状態の永続化
:core:designsystem  必要になれば
```

依存方向: `feature → core:data → core:compiler → core:sim`。`:core:sim` は Android と Remote Compose のどちらにも依存しない。

## 理由

- 「Sim と Surface はドキュメントだけで繋がる」境界を、初日からモジュール境界で強制できる（ADR-0002 の狙いはそのまま）
- Surface の分割はスパイクの結果に左右されるため、先に切ると切り直しのリスクがある

## 影響

- スパイク完了時に「feature の切り出し」をタスクとして明示的に行う
- `:core:sim` の Android 非依存は、`kotlin("jvm")` プラグインを使うことでビルド時に保証される
