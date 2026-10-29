package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumEntryDao {
    @Upsert
    suspend fun upsert(entry: AlbumEntryEntity)

    @Query("SELECT * FROM album_entry WHERE age_days = :ageDays")
    suspend fun find(ageDays: Int): AlbumEntryEntity?

    @Query("SELECT * FROM album_entry ORDER BY age_days")
    suspend fun all(): List<AlbumEntryEntity>

    /** 記録した最後の日。まだ何も記録していなければ null。 */
    @Query("SELECT MAX(age_days) FROM album_entry")
    suspend fun lastAgeDays(): Int?

    @Query("DELETE FROM album_entry")
    suspend fun deleteAll()

    /** 記録のある日（経過日数の昇順）。記録や作り直しのたびに流れ直す。 */
    @Query("SELECT age_days FROM album_entry ORDER BY age_days")
    fun observeDays(): Flow<List<Int>>

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
