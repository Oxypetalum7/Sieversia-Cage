# ADR-0007: パッケージ名からアンダースコアを外す

- ステータス: 採択
- 日付: 2026-10-03
- 関連: ADR-0006（これを廃止する案）, ADR-0005（ktlint 導入）

## 背景

ADR-0006 では、ドメイン `gekka-o.xyz` のハイフンを Java の命名規約に従ってアンダースコアに置き換え、`xyz.gekka_o.sieversiacage` とした。ところが骨組みに ktlint（`ktlint_official`）をかけると、全ファイルで `standard:package-name`（Package name must not contain underscore）違反になった。Kotlin 公式の Coding Conventions も「パッケージ名は小文字のみで、アンダースコアを使わない」としている。Java の規約と Kotlin の規約が食い違っている。

## 検討した選択肢

1. **`xyz.gekkao.sieversiacage`（ハイフンを詰める）** — Kotlin の規約と ktlint の両方を満たす。ドメインとの対応は一目では分からなくなる
2. **`xyz.gekka_o.sieversiacage` のまま、ktlint の `package-name` ルールを無効にする** — ドメインとの対応は保てるが、規約からの例外を設定に抱え続けることになる
3. **applicationId だけ `xyz.gekka_o.sieversiacage` にし、Kotlin のパッケージは `xyz.gekkao.sieversiacage` にする** — どちらの規約も満たすが、二つの名前が並ぶので混乱しやすい

## 決定

選択肢1。applicationId・namespace・Kotlin パッケージのすべてを `xyz.gekkao.sieversiacage` にする。

## 理由

- Kotlin プロジェクトとしての規約（公式 Conventions と ktlint）に例外なく従える
- applicationId はまだ一度も公開しておらず、今なら変更のコストがゼロ

## 影響

- ADR-0006 を `廃止（ADR-0007 により）` にする
- ポートフォリオの他の作品でも同じ名前空間 `xyz.gekkao.*` を使う
