package xyz.gekkao.sieversiacage.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xyz.gekkao.sieversiacage.core.data.db.AlbumEntryDao
import xyz.gekkao.sieversiacage.core.data.db.AlbumEntryEntity
import xyz.gekkao.sieversiacage.core.sim.GrowthStage
import xyz.gekkao.sieversiacage.core.sim.PlantState
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 成長アルバムの窓口（ADR-0011 / ADR-0018）。
 *
 * 正のデータは DB の行（Sim の出力と生成メタデータ）で、`.rc` はそこから作り直せるキャッシュ。
 * 書き込みは「DB の行 → `.rc`」の順で行う。途中で落ちても、行が残っていれば `.rc` は作り直せる。
 */
@Singleton
class AlbumRepository
    @Inject
    constructor(
        private val dao: AlbumEntryDao,
        private val files: AlbumDocumentFiles,
        private val generator: AlbumDocumentGenerator,
        private val clock: Clock,
    ) {
        // 起動時の作り直しと、画面からの記録・作り直しが同じ日を同時に書かないようにする
        private val mutex = Mutex()

        /** 1 日分を記録し、作ったドキュメントを返す。同じ日がすでにあれば上書きする。 */
        suspend fun record(state: PlantState): ByteArray = mutex.withLock { write(state) }

        /** 記録のある日（経過日数の昇順）。 */
        suspend fun days(): List<Int> = dao.all().map { it.ageDays }

        /** 記録のある日のドキュメント。`.rc` がなければ作り直す。記録のない日は null。 */
        suspend fun document(ageDays: Int): ByteArray? {
            if (dao.find(ageDays) == null) return null
            return files.load(ageDays) ?: regenerate(ageDays)
        }

        /** プレイヤーが読めなかった日などを、今の Compiler で作り直す。記録のない日は null。 */
        suspend fun regenerate(ageDays: Int): ByteArray? =
            mutex.withLock {
                dao.find(ageDays)?.let { write(it.toPlantState()) }
            }

        /**
         * 起動時の作り直し（ADR-0011）。次の日を今の Compiler で作り直し、記録のない `.rc` を消す。
         *
         * - Compiler の版が今より古い日、Remote Compose の版が今と違う日
         * - `.rc` がない日
         *
         * @return 作り直した日数
         */
        suspend fun regenerateStale(): Int =
            mutex.withLock {
                val entries = dao.all()
                val savedDays = files.savedDays()
                val staleDays =
                    dao.stale(generator.compilerVersion, generator.remoteComposeVersion).map { it.ageDays }.toSet()
                val targets = entries.filter { it.ageDays in staleDays || it.ageDays !in savedDays }
                targets.forEach { write(it.toPlantState()) }

                // スパイクで保存した日や、記録の前に落ちて残ったファイル
                val orphans = savedDays - entries.map { it.ageDays }.toSet()
                orphans.forEach { files.delete(it) }
                targets.size
            }

        // mutex を持った状態で呼ぶ
        private suspend fun write(state: PlantState): ByteArray {
            val bytes = withContext(Dispatchers.Default) { generator.generate(state) }
            dao.upsert(state.toEntity())
            files.save(state.ageDays, bytes)
            return bytes
        }

        private fun PlantState.toEntity() =
            AlbumEntryEntity(
                ageDays = ageDays,
                stage = stage.name,
                remoteComposeVersion = generator.remoteComposeVersion,
                compilerVersion = generator.compilerVersion,
                target = generator.target.name,
                createdAt = clock.millis(),
            )

        private fun AlbumEntryEntity.toPlantState() = PlantState(ageDays = ageDays, stage = GrowthStage.valueOf(stage))
    }
