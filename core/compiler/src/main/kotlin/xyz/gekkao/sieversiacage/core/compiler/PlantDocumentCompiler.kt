package xyz.gekkao.sieversiacage.core.compiler

import xyz.gekkao.sieversiacage.core.sim.PlantState

/** Sim の状態を RemoteCompose ドキュメント（バイト列）にコンパイルする。実装は初日スパイク①で行う。 */
interface PlantDocumentCompiler {
    fun compile(state: PlantState): ByteArray
}
