package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface AlbumEntryDao {
    @Upsert
    suspend fun upsert(entry: AlbumEntryEntity)

    @Query("SELECT * FROM album_entry WHERE age_days = :ageDays")
    suspend fun find(ageDays: Int): AlbumEntryEntity?

    @Query("SELECT * FROM album_entry ORDER BY age_days")
    suspend fun all(): List<AlbumEntryEntity>

    /** 今の版で作っていない日（ADR-0011 の作り直しの対象）。 */
    @Query(
        "SELECT * FROM album_entry " +
            "WHERE compiler_version < :compilerVersion OR remote_compose_version != :remoteComposeVersion " +
            "ORDER BY age_days",
    )
    suspend fun stale(
        compilerVersion: Int,
        remoteComposeVersion: String,
    ): List<AlbumEntryEntity>
}
