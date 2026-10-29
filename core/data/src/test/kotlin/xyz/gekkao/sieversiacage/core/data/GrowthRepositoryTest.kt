package xyz.gekkao.sieversiacage.core.data

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import xyz.gekkao.sieversiacage.core.data.db.GrowthDao
import xyz.gekkao.sieversiacage.core.data.db.PlantEntity
import xyz.gekkao.sieversiacage.core.data.db.WateringLogEntity
import xyz.gekkao.sieversiacage.core.sim.DayInput
import xyz.gekkao.sieversiacage.core.sim.initialState
import xyz.gekkao.sieversiacage.core.sim.simulate
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")

class GrowthRepositoryTest {
    private val dao = FakeGrowthDao()
    private var now: ZonedDateTime = ZonedDateTime.of(2026, 5, 1, 8, 0, 0, 0, TOKYO)

    private fun repository() = GrowthRepository(dao, Clock.fixed(now.toInstant(), TOKYO))

    @Test
    fun `最初に聞かれた日に植えたことになり、そのあとは変わらない`() =
        runTest {
            repository().plantedOn() shouldBe LocalDate.of(2026, 5, 1)

            now = now.plusDays(3)
            repository().plantedOn() shouldBe LocalDate.of(2026, 5, 1)
        }

    @Test
    fun `日付は端末の現地時刻で区切る`() =
        runTest {
            // 東京の 5 月 1 日 8 時は、UTC ではまだ 4 月 30 日
            repository().recordWatering()

            dao.logs.keys shouldBe setOf("2026-05-01")
        }

    @Test
    fun `同じ日のタップは回数を足していく`() =
        runTest {
            val repository = repository()
            repeat(3) { repository.recordWatering() }

            dao.logs["2026-05-01"] shouldBe 3
        }

    @Test
    fun `植えた日の状態は初期状態`() =
        runTest {
            repository().stateOn() shouldBe initialState()
        }

    @Test
    fun `植えた日から前日までの水やりの記録で Sim を回す`() =
        runTest {
            val planted = LocalDate.of(2026, 5, 1)
            repository().plantedOn()
            dao.logs["2026-05-01"] = 2
            dao.logs["2026-05-03"] = 1
            // 当日（5 月 4 日）の水やりは、その日の状態には効かない
            dao.logs["2026-05-04"] = 5

            val state = repository().stateOn(LocalDate.of(2026, 5, 4))

            state shouldBe
                simulate(
                    listOf(
                        DayInput(planted, 2),
                        DayInput(planted.plusDays(1), 0),
                        DayInput(planted.plusDays(2), 1),
                    ),
                )
        }
}

private class FakeGrowthDao : GrowthDao {
    var plantRow: PlantEntity? = null
    val logs = sortedMapOf<String, Int>()

    override suspend fun plant(): PlantEntity? = plantRow

    override suspend fun insertPlantIfAbsent(plant: PlantEntity) {
        if (plantRow == null) plantRow = plant
    }

    override suspend fun insertWateringIfAbsent(log: WateringLogEntity) {
        logs.putIfAbsent(log.date, log.taps)
    }

    override suspend fun incrementWatering(date: String) {
        logs.computeIfPresent(date) { _, taps -> taps + 1 }
    }

    override suspend fun wateringBetween(
        from: String,
        until: String,
    ): List<WateringLogEntity> = logs.subMap(from, until).map { (date, taps) -> WateringLogEntity(date, taps) }
}
