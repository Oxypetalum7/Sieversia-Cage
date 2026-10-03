package xyz.gekkao.sieversiacage.core.sim

// スパイク用の仮の閾値。水やり・季節係数は本実装で入れる
private val stageStartDays: List<Pair<Int, GrowthStage>> =
    listOf(
        0 to GrowthStage.SPROUT,
        3 to GrowthStage.LEAF,
        7 to GrowthStage.BUD,
        10 to GrowthStage.BLOOM,
        14 to GrowthStage.SEED_HEAD,
    )

/** 経過日数から株の状態を決める。 */
fun simulate(ageDays: Int): PlantState {
    require(ageDays >= 0) { "ageDays must be non-negative: $ageDays" }
    val stage = stageStartDays.last { (start, _) -> ageDays >= start }.second
    return PlantState(ageDays = ageDays, stage = stage)
}
