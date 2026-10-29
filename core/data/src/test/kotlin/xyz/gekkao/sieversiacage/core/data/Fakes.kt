package xyz.gekkao.sieversiacage.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import xyz.gekkao.sieversiacage.core.data.db.AlbumEntryDao
import xyz.gekkao.sieversiacage.core.data.db.AlbumEntryEntity
import xyz.gekkao.sieversiacage.core.data.db.GrowthDao
import xyz.gekkao.sieversiacage.core.data.db.PlantEntity
import xyz.gekkao.sieversiacage.core.data.db.WateringLogEntity

// :core:data のテストで共通に使う、メモリの中だけで動く偽の DAO

internal class FakeAlbumEntryDao : AlbumEntryDao {
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

    override suspend fun lastAgeDays(): Int? = rows.value.keys.maxOrNull()

    override suspend fun deleteAll() {
        rows.value = sortedMapOf()
    }

    override suspend fun stale(
        compilerVersion: Int,
        remoteComposeVersion: String,
    ): List<AlbumEntryEntity> =
        rows.value.values.filter {
            it.compilerVersion < compilerVersion || it.remoteComposeVersion != remoteComposeVersion
        }
}

internal class FakeGrowthDao : GrowthDao {
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
