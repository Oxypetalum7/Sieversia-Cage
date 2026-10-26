package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.Database
import androidx.room3.RoomDatabase

/** 成長データの DB（ADR-0018）。版を上げるときは schemas/ の書き出しを残し、マイグレーションとそのテストを書く。 */
@Database(entities = [AlbumEntryEntity::class], version = 1, exportSchema = true)
abstract class SieversiaDatabase : RoomDatabase() {
    abstract fun albumEntryDao(): AlbumEntryDao
}
