package xyz.gekkao.sieversiacage.core.sim

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

// 季節係数が 1.0 の月（5 月）に植えた前提にして、日数の見込みを素直に確かめる
private val MAY_1: LocalDate = LocalDate.of(2026, 5, 1)

class GrowthSimTest {
    @Test
    fun `植えた日は芽で、土は湿っている`() {
        initialState() shouldBe
            PlantState(
                ageDays = 0,
                cycle = 0,
                stage = GrowthStage.SPROUT,
                stageProgress = 0f,
                growth = 0f,
                branchCount = 1,
                flowerCount = 1,
                soilMoisture = 1f,
                vigor = 1f,
            )
    }

    @Test
    fun `毎日水をやると、約 3 週間で綿毛になる`() {
        val firstSeedHead = (0..30).first { simulateWateredDaily(MAY_1, it).stage == GrowthStage.SEED_HEAD }

        firstSeedHead shouldBeInRange 17..21
    }

    @Test
    fun `段階は芽、葉、蕾、開花、綿毛の順に進む`() {
        val stages = (0..20).map { simulateWateredDaily(MAY_1, it).stage }.distinct()

        stages shouldBe
            listOf(GrowthStage.SPROUT, GrowthStage.LEAF, GrowthStage.BUD, GrowthStage.BLOOM, GrowthStage.SEED_HEAD)
    }

    @Test
    fun `綿毛のあとは葉の時期に戻り、周を重ねるごとに枝と花が増える`() {
        val afterFirstLife = simulateWateredDaily(MAY_1, 21)

        afterFirstLife.cycle shouldBe 1
        afterFirstLife.stage shouldBe GrowthStage.LEAF
        afterFirstLife.branchCount shouldBe 2
        afterFirstLife.flowerCount shouldBe 2
    }

    @Test
    fun `2 周目以降は約 2 週間ごとに綿毛になる`() {
        fun isSeedHead(day: Int) = simulateWateredDaily(MAY_1, day).stage == GrowthStage.SEED_HEAD

        // 綿毛になった最初の日（前の日は綿毛でない日）
        val seedHeadDays = (1..80).filter { isSeedHead(it) && !isSeedHead(it - 1) }

        (seedHeadDays[2] - seedHeadDays[1]) shouldBeInRange 12..16
    }

    @Test
    fun `枝と花の数には上限がある`() {
        val old = simulateWateredDaily(MAY_1, 365)

        old.branchCount shouldBe 6
        old.flowerCount shouldBe 5
    }

    @Test
    fun `連打しても 1 回の水やりと同じ`() {
        val once = step(initialState(), DayInput(MAY_1, waterTaps = 1))
        val many = step(initialState(), DayInput(MAY_1, waterTaps = 30))

        many shouldBe once
    }

    @Test
    fun `水をやらない日が 3 日続くと、土が乾ききる`() {
        val dry = (0 until 3).fold(initialState()) { s, i -> step(s, DayInput(MAY_1.plusDays(i.toLong()), 0)) }

        dry.soilMoisture shouldBe 0f
    }

    @Test
    fun `乾いても枯れず、ゆっくり育ち続け、水をやれば元気が戻る`() {
        var state = initialState()
        repeat(30) { state = step(state, DayInput(MAY_1.plusDays(it.toLong()), 0)) }
        val dry = state

        dry.vigor shouldBeGreaterThan -0.0001f
        dry.ageDays shouldBe 30
        // 毎日水をやれば 30 日で 2 周目に入っているが、乾いたままだと最初の一生のまま
        dry.cycle shouldBe 0
        dry.growth shouldBeGreaterThan 3f

        repeat(5) { state = step(state, DayInput(MAY_1.plusDays(30L + it), 1)) }
        state.vigor shouldBeGreaterThan 0.9f
    }

    @Test
    fun `その日の水やりは翌日の成長から効く`() {
        val thirsty = initialState().copy(soilMoisture = 0f, vigor = 0f)

        val watered = step(thirsty, DayInput(MAY_1, waterTaps = 1))
        val notWatered = step(thirsty, DayInput(MAY_1, waterTaps = 0))

        // 今日の成長は、今日の始まりの元気さ（0）で決まるので同じ
        watered.growth shouldBe notWatered.growth
        // 水をやった分は、翌日の元気さに効く
        watered.vigor shouldBeGreaterThan notWatered.vigor
    }

    @Test
    fun `夏は冬より早く育つ`() {
        val summer = simulateWateredDaily(LocalDate.of(2026, 7, 1), 10)
        val winter = simulateWateredDaily(LocalDate.of(2026, 1, 1), 10)

        summer.growth shouldBeGreaterThan winter.growth
    }

    @Test
    fun `季節係数は 0_8 から 1_2 倍`() {
        Month.entries.forEach { month ->
            seasonFactor(month) shouldBeGreaterThan 0.79f
            seasonFactor(month) shouldBeLessThan 1.21f
        }
        seasonFactor(Month.JULY) shouldBe (1.2f plusOrMinus 0.0001f)
    }

    @Test
    fun `負のタップ数と負の経過日数は受け付けない`() {
        shouldThrow<IllegalArgumentException> { DayInput(MAY_1, -1) }
        shouldThrow<IllegalArgumentException> { simulateWateredDaily(MAY_1, -1) }
    }
}
