package xyz.gekkao.sieversiacage.core.sim

/**
 * Sim が出力する株の状態。Compiler 層はこれだけを見てドキュメントを組み立てる。
 *
 * @property ageDays 植えてからの経過日数
 * @property stage 現在の成長段階
 */
data class PlantState(
    val ageDays: Int,
    val stage: GrowthStage,
)
