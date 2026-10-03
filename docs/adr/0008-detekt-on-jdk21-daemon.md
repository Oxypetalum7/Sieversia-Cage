# ADR-0008: detekt は安定版 1.23.8 を使い、Gradle デーモンを JDK 21 に固定する

- ステータス: 採択
- 日付: 2026-10-03
- 関連: ADR-0003（安定版を優先する方針・JDK）, ADR-0005（detekt + compose-rules を初日に導入）

## 背景

ADR-0003 の方針どおり、安定版の detekt 1.23.8 で導入を試みたところ、次の問題が出た。

- detekt 1.23.8（2025-02 が最終リリース）は内部に Kotlin 2.0 のコンパイラを持っていて、それが JDK 25 のバージョン文字列（`25.0.3`）を解釈できずにタスクが落ちる。開発環境の JDK は Android Studio に同梱の JBR 25
- compose-rules は 0.5.0（2025-12）以降すべて detekt 2.0 系 alpha 向けにビルドされている。detekt 1.x 系に対応する最終版は 0.4.28（2025-11）

## 検証（2026-10-03 実施）

意図的に違反を入れた一時ファイルで、2つの構成を比べた。

| 確認項目 | 1.23.8 + compose-rules 0.4.28 / JDK 21 | 2.0.0-alpha.6 + compose-rules 0.6.7 / JDK 25 |
| --- | --- | --- |
| 起動 | 成功 | 成功 |
| MagicNumber の検出 | 検出した | 検出した |
| compose-rules（ModifierMissing）の検出 | 検出した | 検出した |
| Kotlin 2.2+ の when ガード条件の解析 | 正しく解析した | 正しく解析した |
| Kotlin 2.2+ のマルチドル文字列補間の解析 | 正しく解析した | 正しく解析した |
| Kotlin 2.4 の context parameters の解析 | 正しく解析した | （未検証） |
| 型解決付きタスク（`detektMain`） | 成功 | （未検証） |
| 設定ファイルの互換性 | そのまま使えた | ルール名が変わっていた（`UnusedPrivateMember` → `UnusedPrivateFunction`） |

Gradle の Daemon JVM criteria（`gradle/gradle-daemon-jvm.properties`、`toolchainVersion=21`）と foojay-resolver を組み合わせると、オプションなしの `./gradlew` で JDK 21（Amazon Corretto）が `~/.gradle/jdks` に自動でダウンロードされ、デーモンが JDK 21 で起動することも確認した。

## 検討した選択肢

1. **detekt 1.23.8 + compose-rules 0.4.28。Gradle デーモンを JDK 21 に固定する** — すべて安定版で揃い、上の検証はすべて通った。ただし detekt 1.23 系と compose-rules 0.4 系はどちらも更新が止まっている
2. **detekt 2.0.0-alpha.6 + compose-rules 0.6.7。JDK 25 のまま** — 現行のエコシステムに乗れるが、alpha なので設定キーや DSL がまだ変わる
3. **detekt を入れず、compose-rules を ktlint ルールセット版で使う** — 安定版だけで構成できるが、detekt による設計の臭いの検出（複雑度、長いメソッドなど）が失われる

## 決定

選択肢1。

## 理由

- ADR-0003 の「安定版を優先する」方針と矛盾せず、検証した範囲では機能の不足もない
- JDK 21 は LTS で、AGP 9.4 と Kotlin 2.4 の動作環境として問題がない
- デーモンの JDK は Daemon JVM criteria でリポジトリに宣言されるため、マシンごとのパス設定（`org.gradle.java.home`）が要らない

## 影響

- ビルド全体（detekt だけでなく AGP や Kotlin のコンパイルも）が JDK 21 のデーモン上で動く。ADR-0003 の JDK の行は「デーモンは JDK 21（Daemon JVM criteria で固定）、IDE は JBR 25」と読み替える
- `settings.gradle.kts` に foojay-resolver-convention プラグインを追加する
- Android Studio の Gradle 設定が Daemon JVM criteria（Version: 21）に従っていることを確認済み（2026-10-03）
- compose-rules に新しく追加されたルールは使えない
- **見直しの条件**: detekt 2.0 が安定版になったら、2.0 + 最新の compose-rules に移行し、デーモンの JDK 固定を外せるか検討する。移行時には設定キーのリネームへの対応が要る
