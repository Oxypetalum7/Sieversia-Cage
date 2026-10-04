# ADR-0015: アプリは Android 専用とし、DI には Hilt を使う

- ステータス: 採択
- 日付: 2026-10-04
- 関連: ADR-0005（DI の行を置き換える）, ADR-0004, ADR-0016

## 背景

ADR-0005 では、DI に Koin（+ `verify()` テスト）を使うと決めていた。開発者の規約では、DI は「Android 専用なら Hilt、KMP（iOS 視野あり）なら Koin」と分けている。

本作の主役は Remote Compose のウィジェットで、ドキュメントを再生できるのは Android のプレイヤー（androidx のプレイヤーと OS 内蔵プレイヤー）だけである。iOS のウィジェットは WidgetKit という別の仕組みで、Remote Compose のドキュメントを再生する手段はない。発注書でも iOS 対応は全フェーズでスコープ外としている。

## 検討した選択肢

1. **Koin のまま（ADR-0005）** — KMP に移るときに DI を替えずに済む。依存関係のつながりはランタイムに解決されるので、`verify()` テストで補う必要がある
2. **Android 専用に振り切り、Hilt にする** — 依存関係のつながりをコンパイル時に検査できる。`AppWidgetProvider`（`@AndroidEntryPoint`）や WorkManager（`@HiltWorker`）とも組み合わせられる

## 決定

選択肢2。アプリは Android 専用とし、DI には Hilt を使う。KMP は採らない。`:core:sim` は純 Kotlin（JVM）のまま守る。規約の「Android 専用」の列（ログは Timber、日時は `java.time`、画面遷移は Navigation Compose）に合わせる。

## 理由

- iOS で Remote Compose のウィジェットを動かす道がない以上、DI を KMP 向けにしておく利点が小さい
- KMP は採らない。Sim だけを iOS に持っていっても使い道がないため、Sim 層の KMP 化も目標にしない（発注書の「随時の余地」から外す。発注書の作者とも合意済み）
- ただし `:core:sim` は Android にも Remote Compose にも依存しない純 Kotlin（JVM）のまま守る。理由は iOS のためではなく作品のためで、次の4つ（発注書の「Sim層の独立維持」のねらい＝テスト容易性と差し替え余地を含む）
  - JVM だけでテストが速く回る。Sim は成長の計算の中心で、テストを一番厚くしたい場所
  - 「Sim は Surface を知らない」という境界を、ビルド（`kotlin("jvm")`）が強制する
  - 簡易 Sim を、より本格的な Sim（オーキシン創発級）に差し替えられる。Sim の外側が Sim の中身に依存しないので、入れ替えても Compiler 以降は変わらない
  - Phase 2 のサーバー側生成（remote-creation-jvm）でも、同じ Sim を JVM で使える。サーバーの DI はそちらで選ぶ
- Dagger / Hilt は 2.59 で AGP 9 に対応している（AGP 9 には Gradle 9.1 以上が必要）。最小 SDK は 23 で、本作の minSdk 29 を満たす

## 影響

- ADR-0005 の表の「Koin + verify テスト」の行は、この ADR で「Hilt」に置き換わる。ほかの行（MVI、detekt の CI ゲートなど）はそのまま
- Hilt の注釈処理には KSP を使う。AGP 9.4 / Kotlin 2.4 との組み合わせは、feature の切り出しの最初に実際にビルドして確かめる。通らなければこの ADR を見直す
- `:core:sim` に Hilt（`javax.inject` を含む）を入れない。Sim のクラスを注入したいときは、Android 側のモジュールで `@Provides` する
- KMP 向けのライブラリ（kotlinx-datetime など）を、KMP のためだけに選ばない。Sim でも `java.time` を使ってよい
