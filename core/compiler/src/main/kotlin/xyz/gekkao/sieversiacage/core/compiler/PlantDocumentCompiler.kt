package xyz.gekkao.sieversiacage.core.compiler

import xyz.gekkao.sieversiacage.core.sim.PlantState

/** Sim の状態を RemoteCompose ドキュメント（バイト列）にコンパイルする。 */
interface PlantDocumentCompiler {
    fun compile(
        state: PlantState,
        display: DisplayInfo = DisplayInfo(),
    ): ByteArray
}

/**
 * Sim の出力ではないが、描くときに使う情報（ADR-0021）。
 *
 * @property wateredToday 今日、水をやったか。株に効くのは翌日からだが、状態の欄と土の見た目にはすぐ出す
 */
data class DisplayInfo(
    val wateredToday: Boolean = false,
)
