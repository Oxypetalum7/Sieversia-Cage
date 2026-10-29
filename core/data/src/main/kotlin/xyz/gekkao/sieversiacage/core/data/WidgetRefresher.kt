package xyz.gekkao.sieversiacage.core.data

/**
 * ホーム画面のウィジェットを今の状態で作り直す窓口（ADR-0021）。
 * 実装は `:feature:widget` にあり、Hilt でつなぐ（feature 同士は依存しない、ADR-0016）。
 */
interface WidgetRefresher {
    suspend fun refresh()
}
