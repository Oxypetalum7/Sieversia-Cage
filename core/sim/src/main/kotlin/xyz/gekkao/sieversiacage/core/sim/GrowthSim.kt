package xyz.gekkao.sieversiacage.core.sim

import java.time.LocalDate
import java.time.Month

// ADR-0020: 毎日水をやり、季節係数が 1 なら 1 日 1 ポイント。最初の一生は 18 日目に綿毛、21 日目に次の周へ
private const val BASE_GROWTH = 1f

/** 最初の一生（芽から）の段階のしきい値（成長ポイント）。 */
private val FIRST_LIFE: List<Pair<Float, GrowthStage>> =
    listOf(
        0f to GrowthStage.SPROUT,
        4f to GrowthStage.LEAF,
        10f to GrowthStage.BUD,
        14f to GrowthStage.BLOOM,
        18f to GrowthStage.SEED_HEAD,
    )
private const val FIRST_LIFE_LENGTH = 21f

/** 2 周目以降（葉の時期から）の段階のしきい値。約 2 週間で 1 周する。 */
private val LATER_CYCLE: List<Pair<Float, GrowthStage>> =
    listOf(
        0f to GrowthStage.LEAF,
        5f to GrowthStage.BUD,
        8f to GrowthStage.BLOOM,
        11f to GrowthStage.SEED_HEAD,
    )
private const val LATER_CYCLE_LENGTH = 14f

/** 水をやらない日に減る土の湿り気。3 日で乾ききる。 */
private const val DRYING_PER_DAY = 0.35f

/** 元気さが 1 日で土の湿り気に近づく割合。 */
private const val VIGOR_FOLLOW = 0.5f

/** 元気さが 0 でも残る成長の速さ。乾いても止まりかけるだけで、枯れない。 */
private const val MIN_GROWTH_FACTOR = 0.1f

// 季節係数（ADR-0020: 0.8〜1.2 倍）
private const val SEASON_PEAK = 1.2f
private const val SEASON_HIGH = 1.1f
private const val SEASON_MID = 1.0f
private const val SEASON_LOW = 0.9f
private const val SEASON_WINTER = 0.8f

private const val MAX_BRANCHES = 6
private const val MAX_FLOWERS = 5

/** 植えた日の状態。植えるときに水をやった前提で、土は湿っている。 */
fun initialState(): PlantState = stateOf(ageDays = 0, cycle = 0, growth = 0f, soilMoisture = 1f, vigor = 1f)

/**
 * 1 日進める（ADR-0020）。[state] の日の入力 [input] を当てはめて、翌日の状態を返す。
 *
 * その日の成長は、その日の始まりの元気さで決まる。その日の水やりは土の湿り気と元気さを変え、翌日の成長から効く。
 */
fun step(
    state: PlantState,
    input: DayInput,
): PlantState {
    val growthToday = BASE_GROWTH * growthFactor(state.vigor) * seasonFactor(input.date.month)
    var cycle = state.cycle
    var growth = state.growth + growthToday
    val cycleLength = if (cycle == 0) FIRST_LIFE_LENGTH else LATER_CYCLE_LENGTH
    if (growth >= cycleLength) {
        // 綿毛の時期が終わったら、同じ株が葉の時期に戻る（多年生）
        cycle += 1
        growth -= cycleLength
    }
    val soilMoisture = if (input.watered) 1f else (state.soilMoisture - DRYING_PER_DAY).coerceAtLeast(0f)
    val vigor = state.vigor + (soilMoisture - state.vigor) * VIGOR_FOLLOW
    return stateOf(state.ageDays + 1, cycle, growth, soilMoisture, vigor)
}

/** 植えた日から [inputs] を順に当てはめた状態。[inputs] は植えた日から 1 日ずつ並んでいる前提。 */
fun simulate(inputs: List<DayInput>): PlantState = inputs.fold(initialState(), ::step)

/**
 * 植えた日 [plantedOn] から [ageDays] 日、毎日 1 回水をやった状態。
 * 水やりの記録がまだないところ（ショーケースのプレビューなど）で使う。
 */
fun simulateWateredDaily(
    plantedOn: LocalDate,
    ageDays: Int,
): PlantState {
    require(ageDays >= 0) { "ageDays must be non-negative: $ageDays" }
    return simulate(List(ageDays) { DayInput(plantedOn.plusDays(it.toLong()), waterTaps = 1) })
}

/** 季節係数（ADR-0020）。高山で夏に咲く植物なので夏に高く冬に低いが、冬に始めても育つよう控えめにする。 */
internal fun seasonFactor(month: Month): Float =
    when (month) {
        Month.JULY, Month.AUGUST -> SEASON_PEAK
        Month.JUNE, Month.SEPTEMBER -> SEASON_HIGH
        Month.MAY, Month.OCTOBER -> SEASON_MID
        Month.APRIL, Month.NOVEMBER -> SEASON_LOW
        Month.DECEMBER, Month.JANUARY, Month.FEBRUARY, Month.MARCH -> SEASON_WINTER
    }

private fun growthFactor(vigor: Float): Float = MIN_GROWTH_FACTOR + (1f - MIN_GROWTH_FACTOR) * vigor

private fun stateOf(
    ageDays: Int,
    cycle: Int,
    growth: Float,
    soilMoisture: Float,
    vigor: Float,
): PlantState {
    val stages = if (cycle == 0) FIRST_LIFE else LATER_CYCLE
    val cycleLength = if (cycle == 0) FIRST_LIFE_LENGTH else LATER_CYCLE_LENGTH
    val index = stages.indexOfLast { (start, _) -> growth >= start }
    val start = stages[index].first
    val end = stages.getOrNull(index + 1)?.first ?: cycleLength
    return PlantState(
        ageDays = ageDays,
        cycle = cycle,
        stage = stages[index].second,
        stageProgress = ((growth - start) / (end - start)).coerceIn(0f, 1f),
        growth = growth,
        branchCount = (cycle + 1).coerceAtMost(MAX_BRANCHES),
        flowerCount = (cycle + 1).coerceAtMost(MAX_FLOWERS),
        soilMoisture = soilMoisture,
        vigor = vigor,
    )
}
