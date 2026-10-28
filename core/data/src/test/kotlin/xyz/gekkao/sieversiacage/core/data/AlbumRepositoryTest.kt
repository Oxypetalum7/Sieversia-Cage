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
import xyz.gekkao.sieversiacage.core.sim.simulate
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private const val NOW = 1_000L
private const val RC_VERSION = "1.0.0-alpha20"

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
            val bytes = repository().record(simulate(3))

            dao.find(3) shouldBe
                AlbumEntryEntity(
                    ageDays = 3,
                    stage = "LEAF",
                    remoteComposeVersion = RC_VERSION,
                    compilerVersion = 1,
                    target = "IN_APP",
                    createdAt = NOW,
                )
            files.load(3) shouldBe bytes
        }

    @Test
    fun `rc がない日は読むときに作り直す`() =
        runTest {
            val repository = repository()
            repository.record(simulate(3))
            files.delete(3)

            repository.document(3)?.decodeToString() shouldBe "v1:3:LEAF"
            files.load(3)?.decodeToString() shouldBe "v1:3:LEAF"
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
            repository(FakeGenerator(compilerVersion = 1)).record(simulate(0))
            repository(FakeGenerator(compilerVersion = 2, remoteComposeVersion = "1.0.0-alpha19")).record(simulate(3))
            repository(FakeGenerator(compilerVersion = 2)).record(simulate(7))

            val regenerated = repository(FakeGenerator(compilerVersion = 2)).regenerateStale()

            regenerated shouldBe 2
            dao.all().map { it.compilerVersion to it.remoteComposeVersion } shouldContainExactly
                List(3) { 2 to RC_VERSION }
            files.load(0)?.decodeToString() shouldBe "v2:0:SPROUT"
            files.load(3)?.decodeToString() shouldBe "v2:3:LEAF"
        }

    @Test
    fun `起動時に rc がない日を作り直す`() =
        runTest {
            val repository = repository()
            repository.record(simulate(10))
            files.delete(10)

            repository.regenerateStale() shouldBe 1
            files.load(10)?.decodeToString() shouldBe "v1:10:BLOOM"
        }

    @Test
    fun `起動時に記録のない rc を消す`() =
        runTest {
            val repository = repository()
            repository.record(simulate(0))
            files.save(14, "spike".encodeToByteArray())

            repository.regenerateStale() shouldBe 0
            files.savedDays() shouldContainExactly setOf(0)
        }

    @Test
    fun `作り直しは保存した Sim の出力から作り、Sim を回し直さない`() =
        runTest {
            // Sim の計算式が変わって、今なら 3 日目は LEAF になるとしても、記録した段階を使う
            dao.upsert(simulate(3).toEntityWith(stage = GrowthStage.BUD, compilerVersion = 0))

            repository().regenerate(3)?.decodeToString() shouldBe "v1:3:BUD"
        }

    @Test
    fun `記録した日の一覧は経過日数の順に流れる`() =
        runTest {
            val repository = repository()
            repository.record(simulate(7))
            repository.record(simulate(0))

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

private class FakeAlbumEntryDao : AlbumEntryDao {
    private val rows = MutableStateFlow(sortedMapOf<Int, AlbumEntryEntity>())

    override suspend fun upsert(entry: AlbumEntryEntity) {
        rows.value =
            sortedMapOf<Int, AlbumEntryEntity>().apply {
                putAll(rows.value)
                put(entry.ageDays, entry)
            }
    }

    override suspend fun find(ageDays: Int): AlbumEntryEntity? = rows.value[ageDays]

    override suspend fun all(): List<AlbumEntryEntity> = rows.value.values.toList()

    override fun observeDays(): Flow<List<Int>> = rows.map { it.keys.toList() }

    override suspend fun stale(
        compilerVersion: Int,
        remoteComposeVersion: String,
    ): List<AlbumEntryEntity> =
        rows.value.values.filter {
            it.compilerVersion < compilerVersion || it.remoteComposeVersion != remoteComposeVersion
        }
}

private fun PlantState.toEntityWith(
    stage: GrowthStage,
    compilerVersion: Int,
) = AlbumEntryEntity(
    ageDays = ageDays,
    stage = stage.name,
    remoteComposeVersion = RC_VERSION,
    compilerVersion = compilerVersion,
    target = "IN_APP",
    createdAt = 0L,
)
