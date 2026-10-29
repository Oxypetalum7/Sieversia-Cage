package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * 成長データの DB（ADR-0018）。版を上げるときは schemas/ の書き出しを残し、マイグレーションとそのテストを書く。
 *
 * - 1: album_entry（成長段階と生成メタデータ）
 * - 2: album_entry に Sim の出力の列を足す（ADR-0020）
 * - 3: Sim の入力（watering_log と plant）を足す（ADR-0020）
 */
@Database(
    entities = [AlbumEntryEntity::class, WateringLogEntity::class, PlantEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class SieversiaDatabase : RoomDatabase() {
    abstract fun albumEntryDao(): AlbumEntryDao

    abstract fun growthDao(): GrowthDao
}
