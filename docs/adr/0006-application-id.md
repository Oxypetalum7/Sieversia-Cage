# ADR-0006: applicationId とパッケージ名

- ステータス: 採択
- 日付: 2026-10-03
- 関連: ADR-0004

## 背景

applicationId とルートパッケージ名を決める。開発者は独自ドメイン `gekka-o.xyz` を保有している。

## 検討した選択肢

1. **`io.github.oxypetalum7.sieversiacage`** — GitHub ハンドル由来。ドメインを持たない場合の定番
2. **`xyz.gekka_o.sieversiacage`** — 独自ドメインを逆順にしたもの。ハイフンはパッケージ名に使えないため、Java の命名規約（JLS 6.1）に従いアンダースコアに置換する

## 決定

選択肢2。applicationId・namespace・ルートパッケージをすべて `xyz.gekka_o.sieversiacage` とし、モジュールごとに `xyz.gekka_o.sieversiacage.core.sim` のように後ろへ足していく。

## 理由

- 保有ドメインに紐づくため一意性が確実で、ポートフォリオ全体の名前空間として使い回せる

## 影響

- applicationId は公開後に変更できないため、この値で固定する（Play ストアリリースは現時点でスコープ外）
