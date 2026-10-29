package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * 成長データの DB（ADR-0018）。版を上げるときは schemas/ の書き出しを残し、マイグレーションとそのテストを書く。
 *
 * - 1: album_entry（成長段階と生成メタデータ）
 * - 2: album_entry に Sim の出力の列を足す（ADR-0020）
 */
@Database(
    entities = [AlbumEntryEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class SieversiaDatabase : RoomDatabase() {
    abstract fun albumEntryDao(): AlbumEntryDao
}
