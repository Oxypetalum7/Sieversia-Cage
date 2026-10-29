package xyz.gekkao.sieversiacage.core.data

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import xyz.gekkao.sieversiacage.core.compiler.DocumentTarget
import xyz.gekkao.sieversiacage.core.data.db.PlantEntity
import xyz.gekkao.sieversiacage.core.data.db.THE_PLANT_ID
import xyz.gekkao.sieversiacage.core.sim.PlantState
import xyz.gekkao.sieversiacage.core.sim.simulateWateredDaily
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

private val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")

class GrowthTickerTest {
    @TempDir
    lateinit var dir: File

    private val albumDao = FakeAlbumEntryDao()
    private val growthDao = FakeGrowthDao()
    private val widgets = CountingWidgetRefresher()
    private var now: ZonedDateTime = ZonedDateTime.of(2026, 10, 1, 0, 5, 0, 0, TOKYO)

    private fun ticker(): GrowthTicker {
        val clock = Clock.fixed(now.toInstant(), TOKYO)
        val album = AlbumRepository(albumDao, AlbumDocumentFiles(dir), StateGenerator(), clock)
        return GrowthTicker(GrowthRepository(growthDao, clock), album, widgets)
    }

    @Test
    fun `初めて植えた日は、それより前のアルバムの記録を消して 0 日目を記録する`() =
        runTest {
            // ショーケースのプレビューで記録した日（植える前の記録）
            val preview = AlbumRepository(albumDao, AlbumDocumentFiles(dir), StateGenerator(), Clock.systemUTC())
            preview.record(simulateWateredDaily(now.toLocalDate(), 10))

            ticker().catchUp() shouldBe 1

            albumDao.observeDays().first() shouldContainExactly listOf(0)
            widgets.count shouldBe 1
        }

    @Test
    fun `抜けている日をまとめて記録する`() =
        runTest {
            ticker().catchUp()
            now = now.plusDays(3)

            ticker().catchUp() shouldBe 3

            albumDao.observeDays().first() shouldContainExactly listOf(0, 1, 2, 3)
        }

    @Test
    fun `同じ日に何度呼んでも、二重には記録せず、ウィジェットも作り直さない`() =
        runTest {
            ticker().catchUp()

            ticker().catchUp() shouldBe 0

            albumDao.observeDays().first() shouldContainExactly listOf(0)
            widgets.count shouldBe 1
        }

    @Test
    fun `記録する状態は、その日までの水やりの記録で Sim を回したもの`() =
        runTest {
            growthDao.plantRow = PlantEntity(THE_PLANT_ID, "2026-09-28")
            growthDao.logs["2026-09-28"] = 1

            ticker().catchUp() shouldBe 4

            // 植えた日に水をやり、そのあと 2 日やっていないので、3 日目の土は乾きかけている
            albumDao.find(3)?.soilMoisture shouldBe (0.3f plusOrMinus 0.0001f)
            widgets.count shouldBe 1
        }
}

private class CountingWidgetRefresher : WidgetRefresher {
    var count = 0

    override suspend fun refresh() {
        count++
    }
}

/** 状態をそのまま文字列にする偽の Compiler。 */
private class StateGenerator : AlbumDocumentGenerator {
    override val remoteComposeVersion = "1.0.0-alpha20"
    override val compilerVersion = 1
    override val target = DocumentTarget.IN_APP

    override fun generate(state: PlantState): ByteArray = state.toString().encodeToByteArray()
}
