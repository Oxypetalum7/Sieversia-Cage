package xyz.gekkao.sieversiacage.core.data

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import xyz.gekkao.sieversiacage.core.compiler.DocumentTarget
import xyz.gekkao.sieversiacage.core.data.db.AlbumEntryDao
import xyz.gekkao.sieversiacage.core.data.db.AlbumEntryEntity
import xyz.gekkao.sieversiacage.core.sim.GrowthStage
import xyz.gekkao.sieversiacage.core.sim.PlantState
import xyz.gekkao.sieversiacage.core.sim.simulateWateredDaily
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val NOW = 1_000L
private const val RC_VERSION = "1.0.0-alpha20"

// 季節係数が 1.0 の月に植えた前提。4 日目は葉、10 日目は蕾、14 日目は開花（ADR-0020 のしきい値）
private val PLANTED_ON: LocalDate = LocalDate.of(2026, 5, 1)

private fun state(ageDays: Int) = simulateWateredDaily(PLANTED_ON, ageDays)

class AlbumRepositoryTest {
    @TempDir
    lateinit var dir: File

    private val dao = FakeAlbumEntryDao()
    private val files by lazy { AlbumDocumentFiles(dir) }

    private fun repository(generator: FakeGenerator = FakeGenerator()) =
        AlbumRepository(dao, files, generator, Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC))

    @Test
    fun `記録すると Sim の出力と生成メタデータの行と rc が残る`() =
        runTest {
            val recorded = state(4)
            val bytes = repository().record(recorded)

            dao.find(4) shouldBe
                AlbumEntryEntity(
                    ageDays = 4,
                    stage = "LEAF",
                    cycle = recorded.cycle,
                    stageProgress = recorded.stageProgress,
                    growth = recorded.growth,
                    branchCount = recorded.branchCount,
                    flowerCount = recorded.flowerCount,
                    soilMoisture = recorded.soilMoisture,
                    vigor = recorded.vigor,
                    remoteComposeVersion = RC_VERSION,
                    compilerVersion = 1,
                    target = "IN_APP",
                    createdAt = NOW,
                )
            files.load(4) shouldBe bytes
        }

    @Test
    fun `rc がない日は読むときに作り直す`() =
        runTest {
            val repository = repository()
            repository.record(state(4))
            files.delete(4)

            repository.document(4)?.decodeToString() shouldBe "v1:4:LEAF"
            files.load(4)?.decodeToString() shouldBe "v1:4:LEAF"
        }

    @Test
    fun `記録のない日は rc が残っていても読まない`() =
        runTest {
            files.save(5, "spike".encodeToByteArray())

            repository().document(5).shouldBeNull()
        }

    @Test
    fun `起動時に Compiler の版が古い日と Remote Compose の版が違う日だけを作り直す`() =
        runTest {
            repository(FakeGenerator(compilerVersion = 1)).record(state(0))
            repository(FakeGenerator(compilerVersion = 2, remoteComposeVersion = "1.0.0-alpha19")).record(state(4))
            repository(FakeGenerator(compilerVersion = 2)).record(state(7))

            val regenerated = repository(FakeGenerator(compilerVersion = 2)).regenerateStale()

            regenerated shouldBe 2
            dao.all().map { it.compilerVersion to it.remoteComposeVersion } shouldContainExactly
                List(3) { 2 to RC_VERSION }
            files.load(0)?.decodeToString() shouldBe "v2:0:SPROUT"
            files.load(4)?.decodeToString() shouldBe "v2:4:LEAF"
        }

    @Test
    fun `起動時に rc がない日を作り直す`() =
        runTest {
            val repository = repository()
            repository.record(state(14))
            files.delete(14)

            repository.regenerateStale() shouldBe 1
            files.load(14)?.decodeToString() shouldBe "v1:14:BLOOM"
        }

    @Test
    fun `起動時に記録のない rc を消す`() =
        runTest {
            val repository = repository()
            repository.record(state(0))
            files.save(14, "spike".encodeToByteArray())

            repository.regenerateStale() shouldBe 0
            files.savedDays() shouldContainExactly setOf(0)
        }

    @Test
    fun `作り直しは保存した Sim の出力から作り、Sim を回し直さない`() =
        runTest {
            // Sim の計算式が変わって、今なら 3 日目は芽になるとしても、記録した段階を使う
            dao.upsert(state(3).toEntityWith(stage = GrowthStage.BUD, compilerVersion = 0))

            repository().regenerate(3)?.decodeToString() shouldBe "v1:3:BUD"
        }

    @Test
    fun `記録した日の一覧は経過日数の順に流れる`() =
        runTest {
            val repository = repository()
            repository.record(state(7))
            repository.record(state(0))

            repository.days().first() shouldContainExactly listOf(0, 7)
        }

    @Test
    fun `記録のない日の作り直しは null を返す`() =
        runTest {
            repository().regenerate(9).shouldBeNull()
            files.savedDays().shouldBeEmpty()
        }
}

/** 版と Sim の出力をそのまま文字列にする偽の Compiler。 */
private class FakeGenerator(
    override val compilerVersion: Int = 1,
    override val remoteComposeVersion: String = RC_VERSION,
) : AlbumDocumentGenerator {
    override val target = DocumentTarget.IN_APP

    override fun generate(state: PlantState): ByteArray =
        "v$compilerVersion:${state.ageDays}:${state.stage}".encodeToByteArray()
}

private fun PlantState.toEntityWith(
    stage: GrowthStage,
    compilerVersion: Int,
) = AlbumEntryEntity(
    ageDays = ageDays,
    stage = stage.name,
    cycle = cycle,
    stageProgress = stageProgress,
    growth = growth,
    branchCount = branchCount,
    flowerCount = flowerCount,
    soilMoisture = soilMoisture,
    vigor = vigor,
    remoteComposeVersion = RC_VERSION,
    compilerVersion = compilerVersion,
    target = "IN_APP",
    createdAt = 0L,
)
